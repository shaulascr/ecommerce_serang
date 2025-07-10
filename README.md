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

The app communicates with a custom backend server via REST API and WebSocket.

## Tech Stack
- MVVM architecture
- Retrofit for API communication
- Hilt for dependency injection
- Socket.IO client for real-time chat
- Firebase Cloud Messaging (FCM) for notifications
- Coroutines for async operations
- Glide for image loading
- ViewBinding for UI access
- LiveData and StateFlow for reactive UI
- RajaOngkir API integration for shipping cost

## Project Structure
  - api/retrofit/  
  - data/   
  - di/   
  - ui/  
    - auth/  
    - home/ 
    - cart/ 
    - order/  
      - history/
      - review/
    - chat/ 
    - profile/
      - store/
        - addProduct/
        - sells/
        - balance/
        - review/
    - product/ 
    - notif/  
  - utils/ 
  - google-services.json 

## How to Run

1. Clone this project and open it in Android Studio
2. Add your `google-services.json` for Firebase (FCM)
3. Update the API base URL in the Retrofit client
5. Settings BASE_URL in your local.properties
4. Build and run on an emulator or physical device


