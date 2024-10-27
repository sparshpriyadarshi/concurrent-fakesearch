#!/bin/bash
echo "Running..."

COMBINED_OUTPUT_FILE="output.csv"
HEADER_LINE=$'run_type,search_version,results,time_ms\n'

touch $COMBINED_OUTPUT_FILE
>$COMBINED_OUTPUT_FILE #clear it
echo $HEADER_LINE>>$COMBINED_OUTPUT_FILE #header
go run fakesearch-go/main.go >>$COMBINED_OUTPUT_FILE
java fakesearch-java/Main.java >>$COMBINED_OUTPUT_FILE

echo "...Complete"
echo "file written: $COMBINED_OUTPUT_FILE"

read -n 1 -s -r -p "Press any key to exit"
