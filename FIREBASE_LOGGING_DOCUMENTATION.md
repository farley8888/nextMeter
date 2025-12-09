# Firebase Logging Documentation

This document explains all Firebase logs in the Cable Meter Android application. All logs are written to the Firestore collection: `meters/{meter_id}/loggings`

---

## Table of Contents
1. [Page Navigation Logs](#1-page-navigation-logs)
2. [Meter Locked Logs](#2-meter-locked-logs)
3. [4G Module Restarting Logs](#3-4g-module-restarting-logs)
4. [ACC Status Change Logs](#4-acc-status-change-logs)
5. [ACC Debounce Check Logs](#5-acc-debounce-check-logs)
6. [SIM State Change Logs](#6-sim-state-change-logs)
7. [Key Press Logs](#7-key-press-logs)
8. [Pulse Counter Logs](#8-pulse-counter-logs)

---

## 1. Page Navigation Logs

**Log Type:** `page_navigation`

**When It Happens:**
- Triggered every time the user navigates from one screen to another in the app
- Only logged to Firebase when `is_enabled_page_log = true` in Remote Config
- Always logged to Android logcat regardless of the flag

**Location in Code:**
- `app/src/main/java/com/vismo/nextgenmeter/repository/NavigationLogger.kt:65-83`
- `app/src/main/java/com/vismo/nextgenmeter/ui/NavigationObserver.kt:26-40`

**Example Log:**
```json
{
  "log_type": "page_navigation",
  "from_page": "meterOps",
  "to_page": "tripSummaryDashboard",
  "timestamp": "2025-12-09T10:30:45.123Z",
  "meter_identifier": "ABC123",
  "device_id": "device_xyz_001"
}
```

**Field Descriptions:**

| Field | Type | Description |
|-------|------|-------------|
| `log_type` | String | Always `"page_navigation"` - identifies this as a navigation log |
| `from_page` | String | The screen/page the user is navigating FROM. Value is `"initial"` if this is the first screen on app launch. <br><br>**Possible values:**<br>- `"initial"` - App just started<br>- `"splash"` - Splash screen<br>- `"meterOps"` - Main meter operations screen<br>- `"pair"` - Driver pairing screen<br>- `"tripHistory"` - Trip history list<br>- `"tripSummaryDashboard"` - Trip summary dashboard<br>- `"mcuSummaryDashboard"` - MCU (Measure Control Unit) summary<br>- `"systemPin"` - PIN entry screen for admin access<br>- `"adminBasicEdit"` - Admin settings (K-value, license plate)<br>- `"adminAdvancedEdit"` - Advanced admin settings (fare calculation)<br>- `"adjustBrightnessOrVolume"` - Brightness/volume adjustment screen<br>- `"updateApk"` - APK update screen |
| `to_page` | String | The screen/page the user is navigating TO. Same possible values as `from_page` (except `"initial"`) |
| `timestamp` | Timestamp | Firebase server timestamp when the navigation occurred |
| `meter_identifier` | String | The meter's license plate or identifier (e.g., "HK1234") |
| `device_id` | String | The unique Android device ID |

**Use Cases:**
- Track user navigation patterns
- Identify most/least used screens
- Debug navigation issues
- Analyze user workflow through the app

---

## 2. Meter Locked Logs

**Log Type:** Identified by `action = "meter_locked"`

**When It Happens:**
- Triggered when the meter board detects an abnormal condition and locks the meter
- Two types of locks:
  1. **Abnormal Pulse**: Irregular distance pulse signals detected from the vehicle
  2. **Over Speed**: Vehicle speed exceeds the configured threshold

**Location in Code:**
- `NxGnFirebaseModule/src/main/java/com/vismo/nxgnfirebasemodule/DashManager.kt:359-368`

**Example Log:**
```json
{
  "created_by": "CABLE METER",
  "action": "meter_locked",
  "server_time": "2025-12-09T10:30:45.123Z",
  "device_time": "2025-12-09T10:30:44.890Z",
  "lock_type": "abnormal pulse"
}
```

**Field Descriptions:**

| Field | Type | Description |
|-------|------|-------------|
| `created_by` | String | Always `"CABLE METER"` - identifies the source as the Android meter app |
| `action` | String | Always `"meter_locked"` - indicates the meter has been locked due to abnormal detection |
| `server_time` | Timestamp | Firebase server timestamp when the log was written (accurate, not affected by device time issues) |
| `device_time` | Timestamp | Timestamp from the Android device when the lock occurred (may be inaccurate if device time is wrong) |
| `lock_type` | String | **Type of lock detected:**<br>- `"abnormal pulse"` - Irregular/suspicious distance pulses from the vehicle's speed sensor. This could indicate:<br>&nbsp;&nbsp;• Tampering with the pulse signal<br>&nbsp;&nbsp;• Faulty speed sensor<br>&nbsp;&nbsp;• Electrical interference<br>- `"over speed"` - Vehicle exceeded the maximum allowed speed threshold configured in the meter. This prevents fare calculation at unrealistic speeds |

**What the Numbers Mean:**

**For "abnormal pulse":**
- The meter board monitors distance pulse signals from the vehicle's speed sensor
- Normal pulses occur at regular intervals based on wheel rotation
- Abnormal pulses are detected when:
  - Pulse frequency is irregular or erratic
  - Pulses occur too fast or too slow for the current speed
  - Electronic signal tampering is detected
- Once locked, the meter requires manual intervention (admin unlock) to resume

**For "over speed":**
- The meter has a configured maximum speed threshold (typically 120-150 km/h)
- When speed exceeds this threshold for sustained period, the meter locks
- This prevents fare calculation during unrealistic speeds
- Protects against meter tampering and ensures accurate fare calculation

**Use Cases:**
- Security monitoring - detect potential fare tampering
- Maintenance alerts - identify vehicles with faulty speed sensors
- Driver behavior tracking - flag dangerous speeding
- Audit trail - prove meter security measures are working

---

## 3. 4G Module Restarting Logs

**Log Type:** Identified by `action = "4G_MODULE_RESTARTING"`

**When It Happens:**
- Triggered when the 4G cellular module is restarted
- Common reasons for restart:
  1. No network connectivity for extended period
  2. Module becomes unresponsive
  3. Manual restart triggered by admin/system
  4. Automatic recovery after detecting connection issues

**Location in Code:**
- `NxGnFirebaseModule/src/main/java/com/vismo/nxgnfirebasemodule/DashManager.kt:371-385`

**Example Log:**
```json
{
  "created_by": "CABLE METER",
  "action": "4G_MODULE_RESTARTING",
  "restarted_at": "2025-12-09T10:30:45.123Z",
  "reason": "No network connection for 5 minutes",
  "server_time": "2025-12-09T10:30:46.000Z"
}
```

**Field Descriptions:**

| Field | Type | Description |
|-------|------|-------------|
| `created_by` | String | Always `"CABLE METER"` - identifies the source as the Android meter app |
| `action` | String | Always `"4G_MODULE_RESTARTING"` - indicates the 4G module is being restarted |
| `restarted_at` | Timestamp | Exact timestamp when the restart command was issued to the 4G module |
| `reason` | String | **Human-readable reason for the restart:**<br>Common reasons include:<br>- `"No network connection for X minutes"` - Module couldn't establish/maintain network connection<br>- `"Module unresponsive"` - 4G module stopped responding to commands<br>- `"Manual restart triggered"` - Admin manually initiated restart<br>- `"Connection quality degraded"` - Signal strength or data quality dropped below threshold<br>- `"Scheduled maintenance restart"` - Preventive restart as part of maintenance routine |
| `server_time` | Timestamp | Firebase server timestamp when the log was written (usually 1-2 seconds after `restarted_at` due to network latency) |

**What the Numbers Mean:**

**Time Difference (`server_time` - `restarted_at`):**
- Typically 1-5 seconds
- If difference is large (>30 seconds), indicates:
  - Network was very slow during restart
  - Log was queued and sent later when network recovered
  - Confirms the network issue that triggered the restart

**Restart Patterns:**
- **Single restart**: Normal recovery from temporary network issue
- **Multiple restarts in short time (< 10 minutes)**:
  - Indicates persistent network problem
  - Possible hardware issue with 4G module
  - SIM card problem
  - Poor signal coverage area
- **Daily restarts at same time**: Scheduled maintenance
- **Restarts during vehicle movement**: Network handoff issues between cell towers

**Use Cases:**
- Network reliability monitoring
- Identify areas with poor 4G coverage (if correlated with GPS data)
- Track 4G module hardware health
- Troubleshoot connectivity issues
- Alert for SIM card or carrier problems
- Plan preventive maintenance when restart frequency increases

**Related Actions:**
After a 4G module restart, look for:
- Heartbeat logs resuming
- Trip data sync completing
- Firebase connection re-established

---

## 4. ACC Status Change Logs

**Log Type:** Identified by `action = "acc_status_change"`

**When It Happens:**
- Triggered during the vehicle's ACC (Accessory/Ignition) power state changes
- Tracks the complete shutdown sequence when the vehicle is turned off
- Multiple logs are created during the shutdown process

**Location in Code:**
- `app/src/main/java/com/vismo/nextgenmeter/MainViewModel.kt:809-876`

**Shutdown Sequence:**

When the vehicle's ACC is turned OFF, the meter goes through a multi-stage shutdown process:

### Stage 1: Backlight Off
```json
{
  "created_by": "CABLE METER",
  "action": "acc_status_change",
  "server_time": "2025-12-09T22:00:00.000Z",
  "device_time": "2025-12-09T22:00:00.000Z",
  "acc_status": "turning_off_backlight"
}
```

**When:** Immediately when ACC turns OFF
**What happens:** Screen backlight is turned off to save power
**Time:** Usually within 1 second of ACC OFF

---

### Stage 2: Entering Sleep Mode
```json
{
  "created_by": "CABLE METER",
  "action": "acc_status_change",
  "server_time": "2025-12-09T22:00:01.000Z",
  "device_time": "2025-12-09T22:00:01.000Z",
  "acc_status": "starting_sleep_mode_in 300 seconds"
}
```

**When:** Right after backlight turns off
**What happens:** Countdown begins before entering low power mode
**Number Meaning:** `300 seconds` = 5 minutes (configurable via `acc_off_switch_to_low_power_mode_delay_seconds` in Firebase Remote Config)
**Why the delay:** Allows driver to restart vehicle without full boot sequence if ACC was turned off briefly

---

### Stage 3: Planning Shutdown
```json
{
  "created_by": "CABLE METER",
  "action": "acc_status_change",
  "server_time": "2025-12-09T22:05:01.000Z",
  "device_time": "2025-12-09T22:05:01.000Z",
  "acc_status": "turning off device in 15 minutes"
}
```

**When:** After sleep mode is entered (300 seconds later)
**What happens:** Device is now in low power mode, planning final shutdown
**Number Meaning:** `15 minutes` = Time until complete shutdown (configurable via MCU board's power off delay setting)
**Why the delay:**
- Allows for power state transitions to complete safely
- Gives network time to sync final data
- Prevents data corruption from abrupt power loss

---

### Stage 4: Final Shutdown
```json
{
  "created_by": "CABLE METER",
  "action": "acc_status_change",
  "server_time": "2025-12-09T22:20:01.000Z",
  "device_time": "2025-12-09T22:20:01.000Z",
  "acc_status": "shutting_down"
}
```

**When:** After the 15-minute delay expires
**What happens:** Device sends shutdown notification to measure board and powers off completely
**Final step:** System executes `reboot -p` command to power down

---

### Alternative: Shutdown Disabled
```json
{
  "created_by": "CABLE METER",
  "action": "acc_status_change",
  "server_time": "2025-12-09T22:20:01.000Z",
  "device_time": "2025-12-09T22:20:01.000Z",
  "acc_status": "shutdown_disabled_staying_in_low_power_mode"
}
```

**When:** After the delay, but shutdown is disabled in Remote Config
**What happens:** Device stays in low power mode indefinitely instead of shutting down
**Use case:** Testing, maintenance, or special vehicle configurations

---

**Field Descriptions:**

| Field | Type | Description |
|-------|------|-------------|
| `created_by` | String | Always `"CABLE METER"` |
| `action` | String | Always `"acc_status_change"` |
| `server_time` | Timestamp | Firebase server timestamp when log was written |
| `device_time` | Timestamp | Device timestamp when status changed |
| `acc_status` | String | **Current ACC status/stage:**<br>- `"turning_off_backlight"` - Screen backlight being turned off<br>- `"starting_sleep_mode_in X seconds"` - Countdown to sleep mode (X = configurable delay)<br>- `"turning off device in X minutes"` - Countdown to shutdown (X = configurable delay)<br>- `"shutting_down"` - Final shutdown executing<br>- `"shutdown_disabled_staying_in_low_power_mode"` - Shutdown cancelled, staying in low power mode |

---

**What the Numbers Mean:**

**Delay Times in `acc_status` field:**

1. **"starting_sleep_mode_in X seconds"**
   - **X = 300 seconds (5 minutes)** by default
   - Configurable via: `acc_off_switch_to_low_power_mode_delay_seconds` in Firebase Remote Config
   - **Purpose:** Quick restart grace period
   - **Example:** Driver turns off car to run quick errand, can restart within 5 minutes without full boot

2. **"turning off device in X minutes"**
   - **X = 15 minutes** by default
   - Configurable via: MCU board's `boardShutdownMinsDelayAfterAcc` parameter
   - **Purpose:** Safe shutdown buffer
   - **Example:** Ensures all network operations complete before power off

**Total Shutdown Time:**
- **Minimum:** 300 seconds (5 min) + 15 minutes = **20 minutes** from ACC OFF to complete shutdown
- **Configurable range:** Can be adjusted from 5 minutes to several hours depending on operational needs

**Time Calculations:**

If you see these logs:
```
22:00:00 - turning_off_backlight
22:00:01 - starting_sleep_mode_in 300 seconds
22:05:01 - turning off device in 15 minutes
22:20:01 - shutting_down
```

**Analysis:**
- Backlight off delay: 1 second (22:00:01 - 22:00:00)
- Sleep mode delay: 300 seconds = 5 minutes (22:05:01 - 22:00:01)
- Shutdown delay: 900 seconds = 15 minutes (22:20:01 - 22:05:01)
- **Total time from ACC OFF to shutdown: 20 minutes**

---

**Use Cases:**
- **Power management monitoring**: Verify shutdown sequence completes correctly
- **Battery drain analysis**: Identify vehicles with abnormal shutdown delays
- **Driver behavior tracking**: See how long vehicles are turned off
- **Debugging power issues**: Detect if shutdown is interrupted or fails
- **Configuration validation**: Confirm custom delay settings are applied correctly
- **Maintenance scheduling**: Identify vehicles that don't complete full shutdown cycles

**Common Patterns:**

1. **Normal Shutdown:**
   ```
   turning_off_backlight → starting_sleep_mode_in 300s → turning off device in 15min → shutting_down
   ```

2. **Quick Restart (ACC ON before shutdown):**
   ```
   turning_off_backlight → starting_sleep_mode_in 300s → [ACC ON - sequence cancelled]
   ```

3. **Shutdown Disabled:**
   ```
   turning_off_backlight → starting_sleep_mode_in 300s → turning off device in 15min → shutdown_disabled_staying_in_low_power_mode
   ```

4. **Configuration Error (unusual timing):**
   ```
   turning_off_backlight → starting_sleep_mode_in 60s → turning off device in 1min
   ```
   *(Indicates custom or possibly incorrect configuration)*

---

## 5. ACC Debounce Check Logs

**Log Type:** Identified by `action = "ACC_DEBOUNCE_CHECK"` or `"ACC_DEBOUNCE_CHECK_DETAILED"`

**When It Happens:**
- Triggered when the ACC (Accessory/Ignition) signal is read and processed
- The system reads the ACC signal multiple times to avoid false positives from electrical noise
- "Debouncing" means taking multiple readings and only accepting the status when it's stable
- Logs are created when:
  1. ACC status changes (awake ↔ sleeping)
  2. Signal is unstable (lots of fluctuation)
  3. Configured for detailed logging via `is_enabled_detail_acc_log = true`

**Location in Code:**
- `app/src/main/java/com/vismo/nextgenmeter/util/ShellStateUtil.kt:100-134`

**Example Log (Standard):**
```json
{
  "created_by": "CABLE METER",
  "action": "ACC_DEBOUNCE_CHECK",
  "server_time": "2025-12-09T10:30:45.123Z",
  "device_time": "2025-12-09T10:30:45.000Z",
  "final_status": "sleeping",
  "previous_status": "awake",
  "true_count": 485,
  "false_count": 15,
  "status_changes": 3,
  "stability": 97.0,
  "total_readings": 500,
  "debounce_count_configured": 500,
  "log_reason": "status_changed"
}
```

**Example Log (Detailed):**
```json
{
  "created_by": "CABLE METER",
  "action": "ACC_DEBOUNCE_CHECK_DETAILED",
  "server_time": "2025-12-09T10:30:45.123Z",
  "device_time": "2025-12-09T10:30:45.000Z",
  "final_status": "sleeping",
  "previous_status": "awake",
  "true_count": 485,
  "false_count": 15,
  "status_changes": 3,
  "stability": 97.0,
  "total_readings": 500,
  "debounce_count_configured": 500,
  "log_reason": "status_changed",
  "first_10_readings": "[true, true, false, true, true, true, true, true, true, true]",
  "last_10_readings": "[true, true, true, true, true, true, true, true, true, true]"
}
```

**Field Descriptions:**

| Field | Type | Description |
|-------|------|-------------|
| `created_by` | String | Always `"CABLE METER"` |
| `action` | String | `"ACC_DEBOUNCE_CHECK"` for standard logs, `"ACC_DEBOUNCE_CHECK_DETAILED"` when detail logging enabled |
| `server_time` | Timestamp | Firebase server timestamp |
| `device_time` | Timestamp | Device timestamp |
| `final_status` | String | **Final determined ACC status after debouncing:**<br>- `"sleeping"` - ACC is OFF (vehicle turned off, true readings)<br>- `"awake"` - ACC is ON (vehicle running, false readings) |
| `previous_status` | String | Previous ACC status before this check: `"sleeping"`, `"awake"`, or `"unknown"` (first reading) |
| `true_count` | Number | **How many readings indicated "sleeping" (ACC OFF)**<br>- Higher number = more readings said ACC is OFF<br>- If this is close to `total_readings`, signal is stable at OFF |
| `false_count` | Number | **How many readings indicated "awake" (ACC ON)**<br>- Higher number = more readings said ACC is ON<br>- If this is close to `total_readings`, signal is stable at ON |
| `status_changes` | Number | **How many times the reading flipped between true/false**<br>- 0-2 = Very stable signal (good)<br>- 3-10 = Some noise but acceptable<br>- 10+ = Unstable signal, possible electrical issues |
| `stability` | Number | **Percentage of how stable the signal is (0-100%)**<br>Calculated as: `(total_readings - status_changes) / total_readings * 100`<br>- 95-100% = Excellent, clean signal<br>- 80-95% = Good, minor noise<br>- 60-80% = Fair, noticeable noise<br>- <60% = Poor, significant electrical interference |
| `total_readings` | Number | Total number of ACC signal readings taken (should match `debounce_count_configured`) |
| `debounce_count_configured` | Number | **Configured number of readings to take**<br>- Default: 500 readings<br>- Configurable via `acc_debounce_count` in Firebase Remote Config<br>- Higher number = more accurate but slower response<br>- Lower number = faster but more false positives |
| `log_reason` | String | **Why this log was created:**<br>- `"status_changed"` - ACC status changed from previous state<br>- `"unstable_signal"` - Signal was fluctuating too much (high `status_changes`)<br>- `"stable_signal"` - Normal stable reading (only logged if detailed logging enabled) |
| `first_10_readings` | String | (Detailed only) First 10 raw readings: `[true, false, true, ...]` - Shows initial signal pattern |
| `last_10_readings` | String | (Detailed only) Last 10 raw readings - Shows final signal pattern |

---

**What the Numbers Mean:**

**Interpreting ACC Status:**
- The ACC signal is read from a GPIO pin on the Android device
- `true` = ACC OFF (sleeping/vehicle turned off)
- `false` = ACC ON (awake/vehicle running)

**Example 1: Clean Status Change (Vehicle Turned Off)**
```json
{
  "final_status": "sleeping",
  "previous_status": "awake",
  "true_count": 498,
  "false_count": 2,
  "status_changes": 1,
  "stability": 99.8,
  "total_readings": 500
}
```
**Analysis:**
- 498 out of 500 readings said "sleeping" (ACC OFF)
- Only 1 status change = signal flipped once from awake to sleeping
- 99.8% stability = excellent, clean signal
- **This is a normal, healthy vehicle turn-off**

---

**Example 2: Noisy Signal**
```json
{
  "final_status": "sleeping",
  "previous_status": "awake",
  "true_count": 450,
  "false_count": 50,
  "status_changes": 45,
  "stability": 91.0,
  "total_readings": 500
}
```
**Analysis:**
- 450 readings said "sleeping", but 50 said "awake"
- 45 status changes = signal flipped back and forth 45 times!
- 91% stability = acceptable but concerning
- **Possible issues:**
  - Loose ACC wire connection
  - Electrical interference from other components
  - Faulty ignition switch
  - Need to check vehicle wiring

---

**Example 3: Severe Electrical Problem**
```json
{
  "final_status": "sleeping",
  "previous_status": "awake",
  "true_count": 250,
  "false_count": 250,
  "status_changes": 200,
  "stability": 60.0,
  "total_readings": 500
}
```
**Analysis:**
- Exactly 50/50 split between sleeping and awake readings
- 200 status changes = signal is extremely unstable
- 60% stability = poor signal quality
- **Critical issues:**
  - ACC wire is damaged or disconnected
  - Serious electrical interference
  - Faulty GPIO pin on Android board
  - **Meter may not function properly - maintenance required**

---

**Debounce Count Configuration:**

The `debounce_count_configured` value determines how many readings are taken:

| Count | Response Time | Accuracy | Use Case |
|-------|--------------|----------|----------|
| 100 | ~100ms | Lower | Testing only |
| 300 | ~300ms | Good | Low-noise environments |
| 500 | ~500ms | Excellent | **Default - recommended** |
| 1000 | ~1000ms | Maximum | High-interference vehicles |

**Formula:** Each reading takes ~1ms, so 500 readings = ~500ms response time

---

**Use Cases:**
- **Electrical health monitoring**: Track `stability` percentage over time
- **Maintenance alerts**: Flag vehicles with `stability < 80%` for wiring inspection
- **Debounce tuning**: Adjust `acc_debounce_count` based on `status_changes` patterns
- **False positive debugging**: Use detailed logs to see exact reading patterns
- **Warranty claims**: Prove electrical issues with historical stability data

**Detailed Logging:**
- Controlled by `is_enabled_detail_acc_log` flag in Remote Config
- When enabled: Logs include `first_10_readings` and `last_10_readings`
- Use for debugging specific vehicles with ACC issues
- **Warning:** Generates much more data in Firebase

---

## 6. SIM State Change Logs

**Log Type:** Identified by `action = "SIM_STATE_CHANGE"`

**When It Happens:**
- Triggered when the SIM card state changes
- Monitors SIM card health and connectivity
- Logs created when:
  1. SIM state changes (e.g., READY → ABSENT)
  2. SIM problem status changes (working → has problem)
  3. SIM card is removed or inserted

**Location in Code:**
- `app/src/main/java/com/vismo/nextgenmeter/service/SimCardStateReceiver.kt:133-149`

**Example Log:**
```json
{
  "created_by": "CABLE METER",
  "action": "SIM_STATE_CHANGE",
  "server_time": "2025-12-09T10:30:45.123Z",
  "device_time": "2025-12-09T10:30:45.000Z",
  "sim_state": "ABSENT",
  "previous_sim_state": "READY",
  "reason": "SIM_STATE_CHANGED",
  "has_problem": true,
  "previous_has_problem": false,
  "state_changed": true,
  "problem_status_changed": true,
  "log_reason": "state_changed_with_problem"
}
```

**Field Descriptions:**

| Field | Type | Description |
|-------|------|-------------|
| `created_by` | String | Always `"CABLE METER"` |
| `action` | String | Always `"SIM_STATE_CHANGE"` |
| `server_time` | Timestamp | Firebase server timestamp |
| `device_time` | Timestamp | Device timestamp |
| `sim_state` | String | **Current SIM card state:**<br>- `"UNKNOWN"` - State cannot be determined<br>- `"ABSENT"` - No SIM card detected<br>- `"PIN_REQUIRED"` - SIM locked, needs PIN<br>- `"PUK_REQUIRED"` - SIM locked, needs PUK code<br>- `"NETWORK_LOCKED"` - SIM locked to specific carrier<br>- `"READY"` - SIM is working normally<br>- `"NOT_READY"` - SIM present but not initialized<br>- `"PERM_DISABLED"` - SIM permanently disabled<br>- `"CARD_IO_ERROR"` - Hardware error reading SIM<br>- `"CARD_RESTRICTED"` - SIM restricted by carrier |
| `previous_sim_state` | String | Previous SIM state (same values as above, or `"unknown"` if first check) |
| `reason` | String | **Android system reason for the state change:**<br>- `"SIM_STATE_CHANGED"` - Generic SIM state change<br>- `"SIM_ABSENT"` - SIM card removed<br>- `"LOADED"` - SIM card loaded successfully<br>- Or other Android broadcast reasons |
| `has_problem` | Boolean | **true** if current state indicates a problem (not READY or UNKNOWN)<br>**false** if SIM is working normally |
| `previous_has_problem` | Boolean | Whether the previous state had a problem |
| `state_changed` | Boolean | **true** if `sim_state` is different from `previous_sim_state`<br>**false** if state is the same |
| `problem_status_changed` | Boolean | **true** if `has_problem` is different from `previous_has_problem`<br>**false** if problem status is the same |
| `log_reason` | String | **Why this log was created (combination of conditions):**<br>- `"state_changed_with_problem"` - State changed AND has problem now<br>- `"state_changed"` - State changed but no problem<br>- `"problem_status_changed_with_problem"` - Problem status changed AND has problem<br>- `"problem_status_changed"` - Problem status changed but no problem now<br>- `"has_problem"` - Fallback when SIM has a problem<br>- `"unknown"` - Shouldn't happen normally |

---

**What the States Mean:**

**Normal States:**
1. **READY** - SIM is working perfectly
   - Can make calls, send SMS, use data
   - This is the desired state

2. **UNKNOWN** - State is not yet determined
   - Usually temporary during boot
   - May appear briefly when checking SIM

**Problem States:**

3. **ABSENT** - No SIM card in slot
   ```
   Possible causes:
   - SIM card removed
   - SIM not properly inserted
   - SIM slot hardware failure
   ```

4. **PIN_REQUIRED** - SIM locked with PIN
   ```
   Possible causes:
   - SIM PIN enabled and device rebooted
   - Wrong PIN entered too many times
   Action needed: Enter correct PIN
   ```

5. **PUK_REQUIRED** - SIM locked with PUK
   ```
   Possible causes:
   - Wrong PIN entered 3 times
   Critical: Need carrier's PUK code or SIM is permanently locked
   ```

6. **NETWORK_LOCKED** - SIM carrier locked
   ```
   Possible causes:
   - SIM from different carrier
   - Device is carrier-locked
   Action needed: Use SIM from correct carrier or unlock device
   ```

7. **CARD_IO_ERROR** - Cannot read SIM card
   ```
   Possible causes:
   - Damaged SIM card
   - Faulty SIM slot
   - Dirty/corroded SIM contacts
   Action needed: Clean SIM, try different SIM, or replace SIM slot
   ```

8. **PERM_DISABLED** - SIM permanently disabled
   ```
   Possible causes:
   - Wrong PUK entered too many times
   - Carrier disabled the SIM
   Action needed: Contact carrier for new SIM card
   ```

---

**Common Scenarios:**

**Scenario 1: SIM Card Removed**
```json
{
  "sim_state": "ABSENT",
  "previous_sim_state": "READY",
  "has_problem": true,
  "previous_has_problem": false,
  "state_changed": true,
  "problem_status_changed": true,
  "log_reason": "state_changed_with_problem"
}
```
**What happened:** SIM card was removed from device
**Impact:** No cellular connectivity, can't communicate with server
**Action:** Reinsert SIM card

---

**Scenario 2: SIM Card Inserted**
```json
{
  "sim_state": "READY",
  "previous_sim_state": "ABSENT",
  "has_problem": false,
  "previous_has_problem": true,
  "state_changed": true,
  "problem_status_changed": true,
  "log_reason": "state_changed"
}
```
**What happened:** SIM card inserted and initialized successfully
**Impact:** Cellular connectivity restored
**Action:** None needed

---

**Scenario 3: SIM PIN Required (after reboot)**
```json
{
  "sim_state": "PIN_REQUIRED",
  "previous_sim_state": "UNKNOWN",
  "has_problem": true,
  "previous_has_problem": false,
  "state_changed": true,
  "problem_status_changed": true,
  "log_reason": "state_changed_with_problem"
}
```
**What happened:** Device rebooted with PIN-protected SIM
**Impact:** No connectivity until PIN entered
**Action:** Need to disable SIM PIN or configure auto-PIN entry

---

**Scenario 4: Critical - PUK Required**
```json
{
  "sim_state": "PUK_REQUIRED",
  "previous_sim_state": "PIN_REQUIRED",
  "has_problem": true,
  "previous_has_problem": true,
  "state_changed": true,
  "problem_status_changed": false,
  "log_reason": "state_changed_with_problem"
}
```
**What happened:** Wrong PIN entered multiple times
**Impact:** SIM is locked, need PUK code from carrier
**Action:** **URGENT** - Contact carrier for PUK code within limited attempts

---

**Use Cases:**
- **SIM health monitoring**: Track SIM state over time
- **Connectivity debugging**: Identify why meter can't connect to network
- **Theft detection**: Unexpected ABSENT state may indicate tampering
- **Maintenance alerts**: Flag vehicles with recurring SIM problems
- **Carrier issues**: Identify NETWORK_LOCKED for wrong SIM cards
- **Hardware failures**: Recurring CARD_IO_ERROR indicates faulty SIM slot

---

## 7. Key Press Logs

**Log Type:** Identified by `action = "KEY_PRESS"`

**When It Happens:**
- Triggered when physical buttons on the meter are pressed
- Only logs if `is_enabled_key_log = true` in Remote Config
- Tracks driver/operator interactions with the meter hardware

**Location in Code:**
- `app/src/main/java/com/vismo/nextgenmeter/ui/meter/MeterOpsViewModel.kt:346-357`

**Example Log:**
```json
{
  "created_by": "cable_meter",
  "action": "KEY_PRESS",
  "server_time": "2025-12-09T10:30:45.123Z",
  "device_time": "2025-12-09T10:30:45.000Z",
  "key_code": 249,
  "key_name": "Start/Resume trip",
  "repeat_count": 1,
  "is_long_press": false
}
```

**Field Descriptions:**

| Field | Type | Description |
|-------|------|-------------|
| `created_by` | String | Always `"cable_meter"` (lowercase) |
| `action` | String | Always `"KEY_PRESS"` |
| `server_time` | Timestamp | Firebase server timestamp |
| `device_time` | Timestamp | Device timestamp |
| `key_code` | Number | **Hardware key code from the physical button:**<br>Each button has a unique number assigned |
| `key_name` | String | **Human-readable button name** (see table below) |
| `repeat_count` | Number | **How many times the key was pressed in rapid succession**<br>- 1 = Single press<br>- 2+ = Multiple rapid presses or held down |
| `is_long_press` | Boolean | **true** if button was held down for extended time<br>**false** if quick press |

---

**Key Code Mapping:**

| Key Code | Key Name | Function |
|----------|----------|----------|
| 248 | End trip | Ends the current trip |
| 249 | Start/Resume trip | Starts a new trip or resumes paused trip |
| 250 | Pause trip | Pauses the current trip |
| 251 | Settings | Opens settings/admin menu |
| 252 | Toggle flag | Toggles the taxi "HIRED/FOR HIRE" flag |
| 253 | Add extras $10 | Adds $10 to trip extras/surcharges |
| 254 | Add extras $1 | Adds $1 to trip extras/surcharges |
| 255 | Print/Recent trip | Prints receipt or shows recent trip |
| Other | Unknown | Unrecognized key code |

---

**What the Numbers Mean:**

**repeat_count Examples:**

```json
{ "repeat_count": 1, "is_long_press": false }
```
**Analysis:** Single quick button press - normal operation

```json
{ "repeat_count": 5, "is_long_press": false }
```
**Analysis:** Button pressed 5 times rapidly (within 1 second)
- Could be driver frantically pressing button
- May indicate button not responding
- Possible UI lag or system slowness

```json
{ "repeat_count": 1, "is_long_press": true }
```
**Analysis:** Button held down for >1 second
- Often triggers special/admin functions
- May be intentional long-press action

```json
{ "repeat_count": 20, "is_long_press": true }
```
**Analysis:** Button stuck or held down continuously
- **Critical:** Button may be physically stuck
- Could be driver holding button waiting for response
- **Check for hardware issue**

---

**Common Patterns:**

**Pattern 1: Normal Trip Start**
```json
{
  "key_code": 249,
  "key_name": "Start/Resume trip",
  "repeat_count": 1,
  "is_long_press": false
}
```
**Analysis:** Driver pressed Start button once normally

---

**Pattern 2: Frustrated Driver (System Lag)**
```
Time 10:30:00 - { "key_code": 249, "repeat_count": 1 }
Time 10:30:01 - { "key_code": 249, "repeat_count": 1 }
Time 10:30:02 - { "key_code": 249, "repeat_count": 1 }
Time 10:30:03 - { "key_code": 249, "repeat_count": 1 }
```
**Analysis:** Same button pressed 4 times in 3 seconds
- Driver thinks button isn't working
- **Issue:** System is slow to respond
- **Action:** Check app performance, reduce lag

---

**Pattern 3: Stuck Button**
```json
{
  "key_code": 252,
  "key_name": "Toggle flag",
  "repeat_count": 50,
  "is_long_press": true
}
```
**Analysis:** Button generated 50 repeat events
- **Critical hardware issue:** Button is stuck or short-circuited
- **Action:** Inspect button hardware immediately

---

**Pattern 4: Admin Access (Settings Long Press)**
```json
{
  "key_code": 251,
  "key_name": "Settings",
  "repeat_count": 1,
  "is_long_press": true
}
```
**Analysis:** Settings button held for long press
- Normal admin access pattern
- Driver attempting to access admin menu

---

**Use Cases:**
- **Driver behavior analysis**: Track which features drivers use most
- **UI/UX improvement**: Identify buttons with high repeat counts (may need better feedback)
- **Hardware maintenance**: Detect stuck or failing buttons
- **Training needs**: See if drivers struggle with certain operations
- **Fraud detection**: Unusual patterns in extras/surcharge additions
- **Performance monitoring**: High repeat counts indicate slow app response

**Privacy Note:** This logging is controlled by `is_enabled_key_log` flag to respect privacy

---

## 8. Pulse Counter Logs

**Log Type:** Identified by `action = "pulse_counter"`

**When It Happens:**
- Triggered during an active trip when abnormal distance pulses or overspeeding is detected
- Monitors the vehicle's speed sensor pulses for irregularities
- Critical for fraud detection and meter accuracy

**Location in Code:**
- `app/src/main/java/com/vismo/nextgenmeter/repository/TripRepositoryImpl.kt:92-103`

**Example Log:**
```json
{
  "created_by": "CABLE METER",
  "action": "pulse_counter",
  "trip_id": "trip_20251209_103045_ABC123",
  "abnormal_pulse_counter": 5,
  "over_speed_counter": 12,
  "ongoing_measure_board_status": "RUNNING",
  "over_speed_lockup_duration": 180,
  "server_time": "2025-12-09T10:30:45.123Z",
  "device_time": "2025-12-09T10:30:45.000Z"
}
```

**Field Descriptions:**

| Field | Type | Description |
|-------|------|-------------|
| `created_by` | String | Always `"CABLE METER"` |
| `action` | String | Always `"pulse_counter"` |
| `trip_id` | String | Unique identifier for the trip where anomalies were detected |
| `abnormal_pulse_counter` | Number | **Count of abnormal distance pulse detections**<br>- Increments each time irregular pulses are detected<br>- Each increment represents a distinct anomaly event<br>- **0** = No abnormal pulses detected<br>- **1-5** = Minor irregularities, could be road conditions<br>- **5-10** = Moderate concern, investigate sensor<br>- **10+** = Serious issue, possible tampering or sensor failure |
| `over_speed_counter` | Number | **Count of overspeeding detections**<br>- Increments each time vehicle exceeds speed threshold<br>- Each increment represents a period of overspeeding<br>- **0** = No overspeeding<br>- **1-3** = Occasional speeding, normal driving<br>- **3-10** = Frequent speeding, driver behavior issue<br>- **10+** = Excessive speeding, safety concern |
| `ongoing_measure_board_status` | String | **Current status of the measure board (MCU):**<br>- `"IDLE"` - No trip active<br>- `"RUNNING"` - Trip in progress, fare calculating<br>- `"PAUSED"` - Trip paused<br>- `"LOCKED"` - Meter locked due to security issue<br>- `"ERROR"` - Measure board in error state |
| `over_speed_lockup_duration` | Number | **Total time spent overspeeding (in seconds)**<br>- Cumulative duration of all overspeeding events<br>- **Example:** If `over_speed_counter = 3` and `over_speed_lockup_duration = 180`, the vehicle spent 180 seconds (3 minutes) overspeeding across 3 separate events<br>- Average per event = `180 / 3 = 60 seconds per overspeeding event` |
| `server_time` | Timestamp | Firebase server timestamp |
| `device_time` | Timestamp | Device timestamp |

---

**What the Numbers Mean:**

**Abnormal Pulse Counter:**

The meter receives distance pulses from the vehicle's speed sensor (typically one pulse per wheel rotation). Normal pulses are:
- Regular frequency at constant speed
- Proportional to vehicle speed
- Consistent pulse width

Abnormal pulses are detected when:
- Pulse frequency is erratic (random intervals)
- Pulses occur impossibly fast
- Pulse signal is distorted or noisy
- Electronic tampering is suspected

**Example 1: Clean Trip (No Issues)**
```json
{
  "abnormal_pulse_counter": 0,
  "over_speed_counter": 0,
  "over_speed_lockup_duration": 0
}
```
**Analysis:** Perfect trip, no anomalies detected

---

**Example 2: Highway Trip (Normal Speeding)**
```json
{
  "abnormal_pulse_counter": 0,
  "over_speed_counter": 2,
  "over_speed_lockup_duration": 45
}
```
**Analysis:**
- No pulse anomalies (sensor working correctly)
- 2 overspeeding events totaling 45 seconds
- Average per event: 45/2 = 22.5 seconds each
- **Interpretation:** Driver briefly exceeded speed limit on highway, normal behavior

---

**Example 3: Suspected Tampering**
```json
{
  "abnormal_pulse_counter": 15,
  "over_speed_counter": 0,
  "over_speed_lockup_duration": 0
}
```
**Analysis:**
- 15 abnormal pulse detections (high count)
- No overspeeding (speed within limits)
- **Critical:** Pulses are irregular but speed is normal
- **Possible causes:**
  1. **Tampering:** Someone manipulating pulse signal to reduce fare
  2. **Sensor failure:** Faulty speed sensor
  3. **Electrical interference:** Nearby electrical equipment
  4. **Wiring issue:** Damaged or loose connection
- **Action:** Inspect vehicle immediately, check for tampering

---

**Example 4: Reckless Driving**
```json
{
  "abnormal_pulse_counter": 1,
  "over_speed_counter": 25,
  "over_speed_lockup_duration": 900
}
```
**Analysis:**
- 1 minor pulse anomaly (acceptable)
- 25 overspeeding events!
- 900 seconds (15 minutes) total overspeeding
- Average per event: 900/25 = 36 seconds each
- **Interpretation:** Driver consistently speeds throughout trip
- **Action:** Driver behavior warning, possible safety review

---

**Example 5: Severe Tampering Attempt**
```json
{
  "abnormal_pulse_counter": 50,
  "over_speed_counter": 0,
  "ongoing_measure_board_status": "LOCKED",
  "over_speed_lockup_duration": 0
}
```
**Analysis:**
- 50 abnormal pulses (extremely high)
- Meter status is LOCKED
- **Critical Security Event:** Meter detected severe pulse manipulation and locked itself
- **Action:**
  1. Immediate investigation required
  2. Trip fare may be inaccurate
  3. Vehicle flagged for security audit
  4. Possible fraud attempt

---

**Over Speed Threshold:**

The speed threshold is typically configured as:
- **Urban taxis:** 80-100 km/h
- **Highway operation:** 120-140 km/h
- **Configurable** via meter settings

When vehicle exceeds this threshold:
1. `over_speed_counter` increments by 1
2. Timer starts counting `over_speed_lockup_duration`
3. If sustained overspeeding, meter may lock automatically

---

**Calculations:**

**Average Overspeeding Duration Per Event:**
```
Average = over_speed_lockup_duration / over_speed_counter
```

Example:
```json
{
  "over_speed_counter": 8,
  "over_speed_lockup_duration": 320
}
```
Average = 320 / 8 = **40 seconds per overspeeding event**

**Interpretation:**
- Each time driver exceeded threshold, they stayed over for ~40 seconds
- Could be:
  - Highway merging
  - Passing slower vehicles
  - Sustained high-speed driving

---

**Use Cases:**
- **Fraud detection**: High `abnormal_pulse_counter` indicates possible tampering
- **Sensor maintenance**: Recurring abnormal pulses suggest sensor replacement needed
- **Driver safety**: Monitor `over_speed_counter` for dangerous driving
- **Insurance claims**: Prove speeding or meter accuracy during disputes
- **Audit trail**: Document meter security measures are working
- **Fare accuracy**: Correlate abnormal pulses with fare discrepancies
- **Hardware quality**: Track sensor reliability across vehicle fleet

**Critical Alerts:**
- `abnormal_pulse_counter > 10` → Immediate investigation
- `over_speed_counter > 20` → Driver safety review
- `ongoing_measure_board_status = "LOCKED"` → Security incident

---

## Summary Table

| Log Type | Action Value | Frequency | Critical? | Controlled by Flag |
|----------|-------------|-----------|-----------|-------------------|
| Page Navigation | N/A (log_type) | Every screen change | No | `is_enabled_page_log` |
| Meter Locked | `meter_locked` | When tampering/overspeed detected | Yes | No (always logged) |
| 4G Module Restart | `4G_MODULE_RESTARTING` | When network issues occur | Medium | No (always logged) |
| ACC Status Change | `acc_status_change` | During shutdown sequence (4 logs per shutdown) | Medium | No (always logged) |
| ACC Debounce Check | `ACC_DEBOUNCE_CHECK` / `ACC_DEBOUNCE_CHECK_DETAILED` | Every ACC signal read (~every 500ms) | Low | Detailed logs: `is_enabled_detail_acc_log` |
| SIM State Change | `SIM_STATE_CHANGE` | When SIM state changes | Medium | No (always logged) |
| Key Press | `KEY_PRESS` | When physical buttons pressed | Low | `is_enabled_key_log` |
| Pulse Counter | `pulse_counter` | When trip anomalies detected | Yes | No (always logged) |

---

## Configuration Flags

**Firebase Remote Config:** `meters/{meter_id}/configurations/meter_sdk`

```json
{
  "common": {
    "is_enabled_page_log": false,  // Set to true to log page navigation to Firebase
    "is_enabled_key_log": false,   // Set to true to log measure board commands
    ...
  }
}
```

**Note:** Regardless of flags, ALL logs are always written to Android logcat for debugging purposes.

---

## Firebase Collection Structure

```
meters/
  └── {meter_identifier}/
      └── loggings/
          ├── {auto_generated_id_1}  // Page navigation log
          ├── {auto_generated_id_2}  // Meter locked log
          ├── {auto_generated_id_3}  // 4G restart log
          └── {auto_generated_id_4}  // ACC status change log
```

Each log document is automatically assigned a unique ID by Firestore.

---

## Querying Logs

**Example Queries:**

```javascript
// Get all meter locks in last 24 hours
db.collection('meters/ABC123/loggings')
  .where('action', '==', 'meter_locked')
  .where('server_time', '>', yesterday)
  .get()

// Get page navigation logs (if enabled)
db.collection('meters/ABC123/loggings')
  .where('log_type', '==', 'page_navigation')
  .orderBy('timestamp', 'desc')
  .limit(100)
  .get()

// Get all 4G restarts this month
db.collection('meters/ABC123/loggings')
  .where('action', '==', '4G_MODULE_RESTARTING')
  .where('server_time', '>', startOfMonth)
  .get()

// Get shutdown sequence
db.collection('meters/ABC123/loggings')
  .where('action', '==', 'acc_status_change')
  .orderBy('server_time', 'desc')
  .limit(4)  // Gets the last shutdown sequence
  .get()
```

---

## Troubleshooting

**If logs are not appearing:**
1. Check Firebase Remote Config for `is_enabled_page_log` (for page navigation only)
2. Verify network connectivity (logs are queued if offline)
3. Check Android logcat - logs always appear there first
4. Verify meter is properly authenticated with Firebase
5. Check Firestore security rules allow writes to loggings collection

**Android Logcat Tags:**
- Page Navigation: `NavigationLogger`
- Meter Locked: `DashManager`
- 4G Restart: `DashManager`
- ACC Status: `MainViewModel`

---

## Version History

| Date | Version | Changes |
|------|---------|---------|
| 2025-12-09 | 1.0 | Initial documentation with all current log types |

