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


## Project Structure
  - api/retrofit/   # API service and Retrofit client setup
  - data/   # Data layer (DTOs, responses, repositories)
  - di/   # Dependency injection modules (Hilt)
  - ui/   # User interface components
    - auth/    # Authentication
    - home/ 
    - cart/ 
    - order/    # Order management
      - history/
      - review/
    - chat/   # Chat features
    - profile/
      - store/    # Store management
        - addProduct/
        - sells/
        - balance/
        - review/
    - product/ 
    - notif/     # Push notifications handling
  - utils/ 
  - google-services.json    # Firebase configuration for push notifications

## How to Run

1. Clone this project and open it in Android Studio
2. Add your `google-services.json` for Firebase (FCM)
3. Update the API base URL in the Retrofit client
5. Settings BASE_URL in your local.properties
4. Build and run on an emulator or physical device


