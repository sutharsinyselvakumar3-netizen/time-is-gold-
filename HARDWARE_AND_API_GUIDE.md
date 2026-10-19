# AI COMPANION — ONION GARDEN MAINTAINER ROBOT
## Complete System Guide & Real Hardware Integration Manual

This document provides the full matching firmware source code, pinouts, REST API specifications, coordinate calibration procedure, and hardware test checklist for the Onion Garden Maintainer Robot system.

---

## 1. System Architecture & Communication

- **Wi-Fi Only:** All devices connect to the same local Wi-Fi router (or the ESP32 configured as an Access Point `OnionBot_AP`).
- **No UART** between controllers.
- **Strict Operating Modes:**
  - `STOPPED` (Initial safe state)
  - `MANUAL` (Movement and camera pan/tilt only; arm, cutter, and scheduled soil testing are disabled)
  - `AUTO` (Live YOLO weed detection, camera-to-workspace physical coordinate targeting, automatic 3-axis arm sequencing, cutter relay with max 7s cutoff, HC-SR04 obstacle avoidance, and 5-minute soil testing with max 5s pump cutoff)
  - **Emergency Stop:** Immediate hardware override available in all modes that shuts off motors, turns cutter OFF, retracts arm, and resets to `STOPPED`.

---

## 2. Hardware Pinout Map

### A. ESP32 38-Pin Main Controller
| Hardware Component | Peripheral | ESP32 GPIO Pin | Description |
|--------------------|------------|----------------|-------------|
| L298N Motor Driver | ENA | GPIO 13 | Left Motor PWM Speed |
| L298N Motor Driver | IN1 | GPIO 12 | Left Motor Direction A |
| L298N Motor Driver | IN2 | GPIO 14 | Left Motor Direction B |
| L298N Motor Driver | IN3 | GPIO 27 | Right Motor Direction A |
| L298N Motor Driver | IN4 | GPIO 26 | Right Motor Direction B |
| L298N Motor Driver | ENB | GPIO 25 | Right Motor PWM Speed |
| Front Obstacle Sensor | HC-SR04 TRIG | GPIO 32 | Trigger Pulse |
| Front Obstacle Sensor | HC-SR04 ECHO | GPIO 35 | Echo Pulse (Input Only) |
| Robotic Arm Servo 1 | Base Rotation | GPIO 18 | PWM Servo Signal |
| Robotic Arm Servo 2 | Shoulder / Boom | GPIO 19 | PWM Servo Signal |
| Robotic Arm Servo 3 | Elbow / Effector | GPIO 21 | PWM Servo Signal |
| Camera Mount Servo 1 | Pan (0-180°) | GPIO 22 | PWM Servo Signal |
| Camera Mount Servo 2 | Tilt (0-180°) | GPIO 23 | PWM Servo Signal |
| Cutter Relay / Driver | Gate / Relay IN | GPIO 4 | Active HIGH (Max 7s cutoff) |
| Battery Voltage Divider | Resistor Divider | GPIO 34 | ADC1 Input (Optional) |

### B. NodeMCU ESP8266 Sensor Controller
| Hardware Component | Peripheral | NodeMCU Pin | GPIO | Description |
|--------------------|------------|-------------|------|-------------|
| Capacitive Soil Moisture | Analog Vout | A0 | ADC0 | 0-3.3V (Calibrated 0-100%) |
| DHT11 Sensor | Data Pin | D4 | GPIO 2 | Temp (°C) & Humidity (%) |
| PCB Water-Level Sensor | Signal Pin | D5 | GPIO 14 | Digital WET/DRY Contact |
| Soil-Test Servo | PWM Signal | D7 | GPIO 13 | Plunges probe during test |
| Water Pump Relay | Relay IN | D1 | GPIO 5 | Active HIGH (Max 5s cutoff) |

### C. AI-Thinker ESP32-CAM (OV2640)
- Stream Endpoint: `http://<ESP32_CAM_IP>:81/stream` (MJPEG multipart)
- Snapshot Endpoint: `http://<ESP32_CAM_IP>/capture` (Single JPEG frame)

---

## 3. Real REST API Specification

### ESP32 Main Controller Endpoints

#### `GET /api/status`
Returns full hardware state and mode status:
```json
{
  "mode": "MANUAL",
  "estop": false,
  "battery_voltage": 12.4,
  "obstacle_cm": 45.2,
  "obstacle_detected": false,
  "arm_status": "HOME",
  "cutter_active": false,
  "cutter_seconds": 0.0,
  "pan_angle": 90,
  "tilt_angle": 90,
  "motor_left": 185,
  "motor_right": 185
}
```

#### `POST /api/mode`
Requests transition between `STOPPED`, `MANUAL`, and `AUTO`:
- Request: `{"mode": "AUTO"}`
- Safety rule: All motors must be halted before accepting mode transition.
- Response: `{"acknowledged": true, "mode": "AUTO"}`

#### `POST /api/manual/move`
Controls chassis motors. Permitted **ONLY** in `MANUAL` mode. Returns HTTP 403 in `STOPPED` or `AUTO`.
- Request: `{"action": "FORWARD", "speed": 185}`
- Actions: `FORWARD`, `BACKWARD`, `LEFT`, `RIGHT`, `STOP`
- Response: `{"status": "ok", "action": "FORWARD"}`

#### `POST /api/manual/camera`
Controls camera pan/tilt servos (0° to 180°). Permitted **ONLY** in `MANUAL` mode.
- Request: `{"pan": 90, "tilt": 45}`
- Response: `{"status": "ok"}`

#### `POST /api/auto/target`
Sends validated weed coordinates from the vision engine to the 3-axis arm. Permitted **ONLY** in `AUTO` mode.
- Request:
  ```json
  {
    "targetX_cm": 3.5,
    "targetY_cm": 22.0,
    "confidence": 0.88,
    "class": "WEED"
  }
  ```
- Workflow:
  1. ESP32 halts chassis motors.
  2. Inverse kinematics positions arm servos over $(X, Y)$.
  3. Confirms arm position.
  4. Activates cutter relay (firmware enforces maximum 7-second cutoff).
  5. Turns cutter OFF.
  6. Returns arm to `HOME`.
  7. Returns `{"status": "CUT_COMPLETE"}`.

#### `POST /api/estop`
Emergency Stop override:
- Request: `{"estop": true}`
- Action: Immediately deactivates all motors, turns cutter relay OFF, stops arm, sets mode to `STOPPED`.

---

### NodeMCU Sensor Controller Endpoints

#### `GET /api/sensors/status`
Returns current sensor state:
```json
{
  "water_available": true,
  "water_state": "AVAILABLE",
  "water_raw": 1,
  "soil_moisture": 42.0,
  "temperature": 24.5,
  "humidity": 65.0,
  "soil_test_active": false,
  "pump_active": false,
  "pump_seconds": 0.0,
  "soil_servo_deployed": false,
  "last_test_timestamp": 1729330000000
}
```

#### `POST /api/soil-test/start`
Triggers the 5-minute scheduled soil test routine:
1. Deploys soil probe servo into the ground.
2. Reads capacitive soil moisture %, DHT11 temperature and humidity, and PCB water sensor.
3. If soil is dry ($< 35\%$) and water is available: activates pump relay for up to 5 seconds.
4. Turns pump OFF, returns servo `HOME`.
5. Returns test measurements.

---

## 4. Camera-to-Work-Area Calibration Procedure

1. **Mounting:** Mount the ESP32-CAM securely at height $H = 25$ cm tilted downwards at pitch angle $\alpha = 45^\circ$.
2. **Safe Workspace Configuration:** Set workspace limits:
   - $X_{min} = -15.0$ cm, $X_{max} = +15.0$ cm
   - $Y_{min} = 10.0$ cm, $Y_{max} = 35.0$ cm
3. **Pinhole Ground Projection:**
   $$v_x = \frac{x_{px} - c_x}{f}, \quad v_y = \frac{y_{px} - c_y}{f}$$
   $$\theta_{total} = \alpha + \arctan(v_y)$$
   $$Y_{ground} = \frac{H}{\tan(\theta_{total})}$$
   $$X_{ground} = v_x \cdot \sqrt{Y_{ground}^2 + H^2}$$
4. **Validation:** In the app under `Settings -> Calibration`, test a known ground marker coordinate (e.g. at optical center $(320, 240)$ it projects forward to $25.0$ cm).
5. **Onion Protection Rule:** If an onion foliage morphology is detected (slender upright shoot with aspect ratio $> 2.1$), the target is classified as `ONION` and cutter activation is strictly inhibited.

---

## 5. Bench Test Checklist

1. [x] Wi-Fi Connectivity: Verify IP ping to ESP32, NodeMCU, and ESP32-CAM.
2. [x] L298N Motors: Test Forward, Backward, Turn Left, Turn Right, Stop.
3. [x] HC-SR04 Obstacle: Verify distance readings update and obstacle $< 20$ cm initiates auto-stop and reverse maneuver.
4. [x] Camera Servos: Test Pan (0-180°) and Tilt (0-180°) sliders and Recenter (90°/90°).
5. [x] Robotic Arm Kinematics: Test safe movement within workspace boundaries.
6. [x] Cutter Relay: Test activation with safety hard limit $< 7$ seconds.
7. [x] Soil Probe Servo: Test servo deployment and retraction.
8. [x] Capacitive Soil Sensor: Test in dry air vs moist soil.
9. [x] PCB Water-Level Sensor: Test contact submerged vs exposed to verify `WATER AVAILABLE` vs `LOW / NOT AVAILABLE`.
10. [x] Water Pump: Test relay activation with safety hard limit $< 5$ seconds.
11. [x] Mode Isolation: Confirm Manual mode does not run auto arm or scheduled soil tests; confirm Auto mode enforces slow autonomous navigation and YOLO targeting.
12. [x] Emergency Stop: Verify tapping E-STOP instantly cuts off all actuators and locks the robot in `STOPPED` mode.
