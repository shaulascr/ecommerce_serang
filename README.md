# E-Commerce Serang (Android App)

A mobile e-commerce platform built with **Kotlin** (Android) and a backend in **Express**, tailored for small businesses (UMKM). Supports browsing, ordering, chatting, and tracking — with **shipping cost calculation (RajaOngkir)** and **push notifications** via Firebase Cloud Messaging.

---

## Overview

This Android app includes:

- Account registration, login, and OTP verification
- Browsing products by category and store
- Cart and checkout orders
- Shipping cost estimation via RajaOngkir API
- Checkout order, tracking, and status updates
- Real-time buyer–seller chat
- Store registration and product management
- Store Balance as active status
- Top up store balance
- Write rating and feedback for purchased products
- Push notifications for user activity

The app communicates with a custom backend server via REST API and WebSocket. The backend service is currently private and tailored for this project.

---

## Project Structure
app/
├── src/
│   └── main/
│       ├── java/com/yourappname/
│       │   ├── api/
│       │   │   └── retrofit/           # ApiService.kt, Retrofit client setup
│       │   ├── data/
│       │   │   ├── dto/               
│       │   │   ├── response/           
│       │   │   └── repository/         
│       │   ├── di/                     # Hilt dependency injection modules
│       │   ├── ui/
│       │   │   ├── auth/               # Login, register, OTP verification
│       │   │   ├── home/               
│       │   │   ├── cart/               
│       │   │   ├── order/              # Order history, detail, and status
│       │   │   ├── chat/               
│       │   │   ├── profile/            
│       │   │   └── product/            
|       |   |   └── notif/              # Socket.IO client setup and event handling
│       │   ├── utils/                  
│       │   └── App.kt                 
│       └── res/
│           ├── layout/                
│           ├── drawable/               
│           ├── values/                 
│           └── navigation/             
├── google-services.json                # Firebase config for push notifications

## How to Run

1. Clone this project and open it in Android Studio
2. Add your `google-services.json` for Firebase (FCM)
3. Update the API base URL in the Retrofit client
5. Settings BASE_URL in your local.properties
4. Build and run on an emulator or physical device

---

