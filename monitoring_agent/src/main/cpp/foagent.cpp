/*
*  A Simple JVMTI Agent
*
*  compile command: g++ -I${JAVA_HOME}/include/ -I${JAVA_HOME}/include/linux foagent.cpp -fPIC -shared -o foagent.so
*/

#include <jvmti.h>
#include <string>
#include <cstring>
#include <iostream>
#include <fstream>
#include <list>
#include <map>
#include <set>
#include <time.h>
#include <stdlib.h>
#include <jni_md.h>

using namespace std;

static jvmtiEnv *gb_jvmti = NULL;
static jvmtiCapabilities gb_capa;
static jrawMonitorID gb_lock;
static string logfileName = "monitoring_agent.log";

/*
 * Case Sensitive Implementation of startsWith()
 * It checks if the string 'mainStr' starts with given string 'toMatch'
 */
static bool startsWith(string mainStr, string toMatch) {
    // std::string::find returns 0 if toMatch is found at starting
    if(mainStr.find(toMatch) == 0)
        return true;
    else
        return false;
}

static void getLocalTime(char* buf) {
    time_t nowtime;
    struct tm *local;

    nowtime = time(NULL);
    local = localtime(&nowtime);

    strftime(buf, 80, "%Y-%m-%d %H:%M:%S", local);  
}

static void enter_critical_section(jvmtiEnv *jvmti) {
    jvmtiError error;
    error = jvmti->RawMonitorEnter(gb_lock);
}

static void exit_critical_section(jvmtiEnv *jvmti) {
    jvmtiError error;
    error = jvmti->RawMonitorExit(gb_lock);
}

static void JNICALL callbackException(jvmtiEnv *jvmti_env, JNIEnv* env, jthread thr, jmethodID method, jlocation location, jobject exception, jmethodID catch_method, jlocation catch_location) {
    enter_critical_section(gb_jvmti);
    {
        char localTime[80];
        ofstream logFileStream;
        logFileStream.open(logfileName, ios::app);

        char *name,*sig,*gsig;
        jvmtiError error = gb_jvmti->GetMethodName(method, &name, &sig, &gsig);
        if (error != JVMTI_ERROR_NONE) {
            logFileStream << "ERROR: GetMethodName!\n" << endl;
            return;
        }

        jclass declaring_class;
        error = gb_jvmti->GetMethodDeclaringClass(method, &declaring_class);
        error = gb_jvmti->GetClassSignature(declaring_class, &sig, &gsig);

        if (startsWith(sig, "Ljava/") || startsWith(sig, "Lsun/")) {
            return;
        }

        string className = sig;
        getLocalTime(localTime);
        logFileStream << "[Monitoring Agent] " << localTime << " Got an exception from Method: " << className.substr(1, className.length() - 2) << "/" << name << endl;

        if (catch_method == 0) {
            logFileStream << "[Monitoring Agent] " << localTime << " This exception is not handled by business code" << endl;
        } else {
            error = gb_jvmti->GetMethodName(catch_method, &name, &sig, &gsig);
            error = gb_jvmti->GetMethodDeclaringClass(catch_method, &declaring_class);
            error = gb_jvmti->GetClassSignature(declaring_class, &sig, &gsig);
            className = sig;
            getLocalTime(localTime);
            logFileStream << "[Monitoring Agent] " << localTime << " This exception is handled by: " << className.substr(1, className.length() - 2) << "/" << name << endl;
        }

        fflush(stdout);
        logFileStream.close();
    }
    exit_critical_section(gb_jvmti);
}

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
    jvmtiError error = (jvmtiError) 0;
    jvmtiEventCallbacks callbacks;

    jint result = jvm->GetEnv((void **) &gb_jvmti, JVMTI_VERSION);
    if (result != JNI_OK || gb_jvmti == NULL) {
        cout << "ERROR: Unable to access JVMTI!" << endl;
    }
    
    memset(&gb_capa, 0, sizeof(jvmtiCapabilities));
    gb_capa.can_signal_thread = 1;
    gb_capa.can_get_owned_monitor_info = 1;
    gb_capa.can_generate_method_entry_events = 1;
    gb_capa.can_generate_exception_events = 1;
    gb_capa.can_generate_vm_object_alloc_events = 1;
    gb_capa.can_tag_objects = 1; 
    gb_capa.can_get_line_numbers = 1;

    error = gb_jvmti->AddCapabilities(&gb_capa);

    if(error != JVMTI_ERROR_NONE) {
        cout << "ERROR: Can't get JVMTI capabilities" << endl;
        return JNI_ERR;
    }

    memset(&callbacks,0,sizeof(jvmtiEventCallbacks));
    callbacks.Exception = &callbackException;

    error = gb_jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    if(error != JVMTI_ERROR_NONE) {
        cout << "ERROR: Can't set jvmti callback!";
        return JNI_ERR;
    }

    error = gb_jvmti->CreateRawMonitor("agent data", &gb_lock);
    if(error != JVMTI_ERROR_NONE) {
        cout << "ERROR: Can't create raw monitor!" << endl;
        return JNI_ERR;
    }

//    error = gb_jvmti->SetEventNotificationMode(JVMTI_ENABLE,JVMTI_EVENT_VM_INIT, (jthread)NULL);

    error = gb_jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_EXCEPTION, (jthread)NULL);

    return error;
}

JNIEXPORT void JNICALL Agent_OnUnload(JavaVM *vm) {
    // nothing to do
}