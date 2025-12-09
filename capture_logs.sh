#!/bin/bash

# Cable Meter Log Capture Script
# Usage: ./capture_logs.sh [duration_in_seconds] [output_filename]
# Example: ./capture_logs.sh 60 my_logs.txt
# Example: ./capture_logs.sh (captures for 30 seconds by default)

# Configuration
DEVICE_SERIAL="NGSM2406A0388"
PACKAGE_NAME="com.vismo.nextgenmeter"
DEFAULT_DURATION=30
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Parse arguments
DURATION=${1:-$DEFAULT_DURATION}
OUTPUT_FILE=${2:-"cable_meter_logs_${TIMESTAMP}.txt"}

echo "=================================="
echo "Cable Meter Log Capture"
echo "=================================="
echo "Device: $DEVICE_SERIAL"
echo "Duration: ${DURATION} seconds"
echo "Output: $OUTPUT_FILE"
echo "=================================="

# Check if device is connected
if ! adb -s $DEVICE_SERIAL get-state >/dev/null 2>&1; then
    echo "ERROR: Device $DEVICE_SERIAL not connected!"
    echo "Available devices:"
    adb devices
    exit 1
fi

# Clear logcat buffer
echo "Clearing logcat buffer..."
adb -s $DEVICE_SERIAL logcat -c

# Enable Firebase debug logging
echo "Enabling Firebase debug logging..."
adb -s $DEVICE_SERIAL shell setprop log.tag.FA VERBOSE 2>/dev/null
adb -s $DEVICE_SERIAL shell setprop log.tag.FA-SVC VERBOSE 2>/dev/null
adb -s $DEVICE_SERIAL shell setprop log.tag.FirebaseAnalytics VERBOSE 2>/dev/null

echo ""
echo "Starting log capture for ${DURATION} seconds..."
echo "Press Ctrl+C to stop early"
echo ""

# Start timestamp
START_TIME=$(date)

# Capture logs with timeout
timeout ${DURATION}s adb -s $DEVICE_SERIAL logcat -v time > "$OUTPUT_FILE" 2>&1 || {
    # Handle Ctrl+C gracefully
    EXIT_CODE=$?
    if [ $EXIT_CODE -eq 130 ]; then
        echo ""
        echo "Log capture interrupted by user"
    fi
}

# End timestamp
END_TIME=$(date)

# Get log statistics
TOTAL_LINES=$(wc -l < "$OUTPUT_FILE")
FIREBASE_LINES=$(grep -i firebase "$OUTPUT_FILE" | wc -l)
APP_LINES=$(grep "$PACKAGE_NAME" "$OUTPUT_FILE" | wc -l)

echo ""
echo "=================================="
echo "Log Capture Complete!"
echo "=================================="
echo "Start time: $START_TIME"
echo "End time: $END_TIME"
echo "Output file: $OUTPUT_FILE"
echo "Total lines: $TOTAL_LINES"
echo "Firebase logs: $FIREBASE_LINES"
echo "App logs: $APP_LINES"
echo "=================================="
echo ""

# Create filtered logs
FIREBASE_FILE="firebase_only_${TIMESTAMP}.txt"
APP_FILE="app_only_${TIMESTAMP}.txt"

echo "Creating filtered log files..."
grep -i firebase "$OUTPUT_FILE" > "$FIREBASE_FILE" 2>/dev/null
grep "$PACKAGE_NAME" "$OUTPUT_FILE" > "$APP_FILE" 2>/dev/null

echo "✓ Firebase logs: $FIREBASE_FILE ($(wc -l < "$FIREBASE_FILE") lines)"
echo "✓ App logs: $APP_FILE ($(wc -l < "$APP_FILE") lines)"
echo ""

# Show sample of Firebase logs if any
if [ "$FIREBASE_LINES" -gt 0 ]; then
    echo "Sample Firebase logs (first 10 lines):"
    echo "-----------------------------------"
    head -10 "$FIREBASE_FILE"
    echo "-----------------------------------"
fi

echo ""
echo "Done! Log files saved in current directory."
