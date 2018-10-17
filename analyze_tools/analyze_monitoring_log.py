#!/usr/bin/python
# -*- coding: utf-8 -*-
# Filename: analyze_monitoring_log.py

import csv
import sys
import getopt
import time
import logging
import re
import hashlib

LOG_PATH = ''
OUTPUTFILE = '' # default: result.csv

def main():
    handle_args(sys.argv[1:])

    result = analyze_log(LOG_PATH)
    write2csv(filename=OUTPUTFILE, dataset=result)
    logging.info("Analyzing finished")

    # write2csv(filename=OUTPUTFILE, dataset=result)

def handle_args(argv):
    global LOG_PATH
    global OUTPUTFILE

    try:
        opts, args = getopt.getopt(argv, "l:o:", ["log=", "outfile=", "help"])
    except getopt.GetoptError as error:
        logging.error(error)
        print_help_info()
        sys.exit(2)

    for opt, arg in opts:
        if opt == "--help":
            print_help_info()
            sys.exit()
        elif opt in ("-l", "--log"):
            LOG_PATH = arg
        elif opt in ("-o", "--outfile"):
            OUTPUTFILE = arg

    if LOG_PATH == '':
        logging.error("You should use -l or --log to specify your log file path")
        print_help_info()
        sys.exit(2)

    if OUTPUTFILE == '':
        OUTPUTFILE = 'result.csv'
        logging.warning("You didn't specify output file's name, will use default name %s", OUTPUTFILE)

def print_help_info():
    print('')
    print('Monitoring Log Analyzing Tool Help Info')
    print('    analyze_monitoring_log.py -l <log_file_path> [-o <outputfile>]')
    print('or: analyze_monitoring_log.py --log=<log_file_path> [--outfile=<outputfile>]')

def get_md5_key(src):
    m = hashlib.md5()
    m.update(src.encode('UTF-8'))
    return m.hexdigest()

def analyze_log(filepath):
    finding_pattern = re.compile(r'class: ([\w/\$\<\>]+), method: ([\w\$]+), signature: ([\S^.]+), type: ([\w/\$]+)')
    stackinfo_pattern = re.compile(r'\[Monitoring Agent\] Stack info \d, class: ([\w/\$\<\>]+), method: ([\w\$]+), signature: ([\S^.]+)')
    handling_pattern = re.compile(r'is handled by class: ([\w/\$\<\>]+), method: ([\w\$]+), signature: ([\S^.]+)')
    total_count = 0
    result = dict()

    with open(filepath, 'rt') as logfile:
        class_name = ""
        method_name = ""
        method_signature = ""
        exception_type = ""
        count = 0
        handled_by = ""
        distance = 0
        stack_height = 0

        for line in logfile:
            if "Got an exception from " in line:
                match = finding_pattern.search(line)
                class_name = match.group(1)
                method_name = match.group(2)
                method_signature = match.group(3)
                exception_type = match.group(4)
                total_count = total_count + 1

                stackinfo = logfile.readline()
                stack_height = 0
                stack_layers = list()
                fo_point = list()
                while "Stack info" in stackinfo:
                    stack_height = stack_height + 1
                    stack_layers.append(stackinfo)
                    stackinfo = logfile.readline()

                # when while loop ends, the last line should be handling result
                match = handling_pattern.search(stackinfo)
                distance = 0
                if (match):
                    handler_class_name = match.group(1)
                    handler_method_name = match.group(2)
                    handler_method_signature = match.group(3)
                    handled_by = handler_class_name + "/" + handler_method_name + " - " + handler_method_signature
                    for layer in stack_layers:
                        if handler_class_name in layer and handler_method_name in layer:
                            break
                        else:
                            layer_info = stackinfo_pattern.search(layer)
                            fo_point.append(str(distance) + ": " + layer_info.group(1) + "/" + layer_info.group(2) + " - " + layer_info.group(3))
                            distance = distance + 1
                    if distance == 0:
                        fo_point.append(str(distance) + ": " + layer_info.group(1) + "/" + layer_info.group(2) + " - " + layer_info.group(3))
                else:
                    handled_by = "not handled"
                    for index, layer in enumerate(stack_layers):
                        layer_info = stackinfo_pattern.search(layer)
                        fo_point.append(str(index) + ": " + layer_info.group(1) + "/" + layer_info.group(2) + " - " + layer_info.group(3))

                key = get_md5_key(class_name + method_name + exception_type + handled_by)
                if key in result:
                    count = result[key][4]
                    count = count + 1
                    result[key][4] = count
                else:
                    count = 1
                    result[key] = list()
                    result[key].append(class_name)
                    result[key].append(method_name)
                    result[key].append(method_signature)
                    result[key].append(exception_type)
                    result[key].append(count)
                    result[key].append(handled_by)
                    result[key].append(distance)
                    result[key].append(stack_height)
                    result[key].append("; ".join(fo_point))
            else:
                continue
    
    logging.info("exceptions: " + str(len(result)))
    logging.info("total count: " + str(total_count))

    return result

def write2csv(filename, dataset):
    with open(filename, 'w', newline='') as file:
        writer = csv.writer(file)
        writer.writerow(["className", "methodName", "methodSignature", "exceptionType", "count", "handledBy", "distance", "stackHeight", "foPoint"])
        for line in dataset:
            writer.writerow(dataset[line])

if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    main()