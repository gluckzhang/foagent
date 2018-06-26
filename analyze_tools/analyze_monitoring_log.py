#!/usr/bin/python
# -*- coding: utf-8 -*-
# Filename: analyze_monitoring_log.py

import csv
import sys
import getopt
import time
import logging
import re

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

def analyze_log(filepath):
    finding_pattern = re.compile(r'Method: ([\w/\$\<\>]+), type: ([\w/\$]+)')
    handling_pattern = re.compile(r'is handled by: ([\w/\$]+)')
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

                if location + exception in result:
                    count = result[location+exception][2]
                    count = count + 1
                    result[location+exception][2] = count
                else:
                    count = 1
                    result[location+exception] = list()
                    result[location+exception].append(location)
                    result[location+exception].append(exception)
                    result[location+exception].append(count)
                    result[location+exception].append(handled_by)
                    result[location+exception].append(distance)
                    result[location+exception].append(stack_height)

            stackinfo = logfile.readline()
            stack_height = 0
            stack_layers = list()
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
                        distance = distance + 1
            else:
                handled_by = "not handled"

            result[location+exception][3] = handled_by
            result[location+exception][4] = distance
            result[location+exception][5] = stack_height
    return result

def write2csv(filename, dataset):
    with open(filename, 'w', newline='') as file:
        writer = csv.writer(file)
        writer.writerow(["class and method", "exception", "count", "handled by", "distance", "stack height"])
        for line in dataset:
            writer.writerow(dataset[line])

if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    main()