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
    finding_pattern = re.compile(r'Method: ([\w/\$\<\>]+), type: ([\w/\$]+)')
    stackinfo_pattern = re.compile(r'\[Monitoring Agent\] Stack info \d:([\w/\$\<\>]+)')
    handling_pattern = re.compile(r'is handled by: ([\w/\$]+)')
    total_count = 0
    result = dict()

    with open(filepath, 'rt') as logfile:
        location = ""
        exception = ""
        count = 0
        handled_by = ""
        distance = 0
        stack_height = 0

        for line in logfile:
            if "Got an exception from Method" in line:
                match = finding_pattern.search(line)
                location = match.group(1)
                exception = match.group(2)
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
                    handled_by = match.group(1)
                    for layer in stack_layers:
                        if match.group(1) in layer:
                            break
                        else:
                            method_name = stackinfo_pattern.search(layer)
                            fo_point.append(str(distance) + ": " + method_name.group(1))
                            distance = distance + 1
                    if distance == 0:
                        fo_point.append(str(distance) + ": " + match.group(1))

                else:
                    handled_by = "not handled"
                    for index, layer in enumerate(stack_layers):
                        method_name = stackinfo_pattern.search(layer)
                        fo_point.append(str(index) + ": " + method_name.group(1))

                key = get_md5_key(location + exception + handled_by)
                if key in result:
                    count = result[key][2]
                    count = count + 1
                    result[key][2] = count
                else:
                    count = 1
                    result[key] = list()
                    result[key].append(location)
                    result[key].append(exception)
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
        writer.writerow(["class and method", "exception", "count", "handled by", "distance", "stack height", "fo point"])
        for line in dataset:
            writer.writerow(dataset[line])

if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    main()