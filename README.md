# 🔐 Lock-Login App

A fun and creative Android app built in Kotlin that challenges the user to unlock **four unique locks** using a mix of physical activity, camera input, motion sensing, and persistence!

## 🚀 Features

- Custom login mechanism with 4 unique interactive challenges
- Foreground step counter service
- ML Kit integration for cat photo recognition
- Motion sensor lock (shake to unlock)
- A runaway lock that resists unlocking



---

## 🔓 How to Unlock Each Lock

### 🐱 Lock 1 — Take a Picture of a Cat

- Tap the top-left lock.
- Grant camera permission when prompted.
- Capture a picture with your device's back camera.
- The app uses ML Kit to detect whether the image contains a **cat**.
- If a cat is found with confidence > 60%, the lock opens.

### 🏃 Lock 2 — The Runaway Lock

- Tap the top-right lock.
- It will move away every time you try to touch it!
- After **20 failed attempts**, it "gives up" and unlocks itself. 
-

### 📳 Lock 3 — Shake It to Unlock

- Physically shake your phone.
- The lock (bottom-left) opens if enough motion is detected.
- It relocks automatically if the phone is still for more than 3 seconds.

### 👣 Lock 4 — Step Goal

- Walk 1000\*\* steps\*\* while the app is open (or service running) (For testing purposes it is set for 10 steps).

  To change the step goal you can find it in the StepCounterService class on line 27  change the goal to a different number 

- Make sure activity recognition permissions are granted.

- The step counter service runs in the background and updates the lock state when the goal is reached.

🔦 Lock 5 — Turn on the Flashlight

This is the center lock.

Turn on your phone’s flashlight.

The app listens for flashlight status changes using the Camera2 API.

Once the flashlight is turned on, the lock will open.

Once all five locks are open, the app transitions to the SuccessActivity screen which confirms you're logged in 🎉. which confirms you're logged in 🎉.

ed in 🎉.

---

## 📲 Installation & Running

1. Clone the project or download it as ZIP.
2. Open in Android Studio.
3. Run on a physical Android device (some features like sensors/camera/step counting won't work properly on emulator).

---

## 🛠 Permissions Required

- `CAMERA` — to capture and analyze cat photos
- `ACTIVITY_RECOGNITION` — to track physical steps
- `FOREGROUND_SERVICE` & `BODY_SENSORS` — for step counting in background

---

## 🧠 Built With

- Kotlin
- Android SDK (Camera, Motion Sensors, Foreground Service)
- ML Kit Vision (Image Labeling)

---

## 📸 Screenshots / 🎥 Demo Video

https://github.com/user-attachments/assets/2f74b8c4-a734-4a90-a005-fc844642750e

<img width="1042" alt="image" src="https://github.com/user-attachments/assets/5e8c6647-36d5-4565-b2d4-009d9ec120a7" />


---

---

Have fun trying to break into this app 😄

