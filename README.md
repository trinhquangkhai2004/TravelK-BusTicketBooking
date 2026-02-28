# 🚌 TravelK - Bus Ticket Booking System

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![React](https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Redis](https://img.shields.io/badge/redis-%23DD0031.svg?style=for-the-badge&logo=redis&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)

## 📖 Introduction

**TravelK** is a comprehensive, scalable online bus ticket booking platform designed to handle high concurrency and ensure data consistency. It solves real-world problems like **Race Conditions** in seat reservation and provides a seamless user experience with real-time updates.


## 🚀 Key Features

*   **Real-time Seat Booking:** Interactive seat map with instant status updates.
*   **Concurrency Control:** Prevents double-booking (Overbooking) using **Redis Distributed Locking**.
*   **Secure Payments:** Integrated with **VNPay** gateway.
*   **Smart Chatbot:** AI-powered customer support using **Google Gemini AI** (RAG).
*   **Automated Notifications:** Asynchronous email sending via **RabbitMQ**.
*   **Admin Dashboard:** Comprehensive management for trips, buses, stations, and revenue analytics.
*   **System Monitoring:** Real-time health checks and metrics with **Prometheus & Grafana**.

## 🛠️ Tech Stack

### Backend
*   **Core:** Java 21, Spring Boot 3.5.3
*   **Security:** Spring Security, JWT
*   **Database:** MySQL 8.0 
*   **Caching & Locking:** Redis
*   **Message Broker:** RabbitMQ
*   **AI:** Spring AI (Google Gemini)


### Frontend
*   **Framework:** React 18.2.0 (Vite)
*   **Styling:** Tailwind CSS
*   **State Management:** React Hooks
*   **HTTP Client:** Axios

### DevOps & Infrastructure
*   **Containerization:** Docker, Docker Compose
*   **Web Server:** Nginx (Reverse Proxy)
*   **Monitoring:** Prometheus, Grafana
*   **Implemented an automated Continuous Integration (CI) pipeline via GitHub Actions**

## 🏗️ System Architecture

The system follows a layered architecture designed for scalability:

1.  **Client Layer:** React SPA served by Nginx.
2.  **API Gateway / Load Balancer:** Nginx handles routing and static content.
3.  **Application Layer:** Spring Boot Backend handles business logic.
4.  **Data Layer:**
    *   **MySQL:** Persistent storage for users, bookings, trips.
    *   **Redis:** Caching data and Distributed Locks for seat reservation.
5.  **Async Layer:** RabbitMQ handles background tasks (Email, SMS).

## ⚡ Solving the "Double-Booking" Problem

One of the biggest challenges in booking systems is handling **Race Conditions** when multiple users try to book the same seat simultaneously.

**Solution:** I implemented **Distributed Locking** using Redis.
1.  When User A selects a seat, a lock key (`hold:trip:{id}:seat:{num}`) is created in Redis with `SET NX` (Atomic operation).
2.  If User B tries to select the same seat, Redis returns `FALSE` immediately.
3.  The lock has a **TTL (Time-To-Live)** of 10 minutes to prevent deadlocks if User A abandons the session.

## 🔧 Installation & Setup

### Prerequisites
*   Docker & Docker Compose installed.
*   Git.

### Steps
1.  **Clone the repository**
    ```sh
    git clone https://github.com/trinhquangkhai2004/TravelK-BusTicketBooking
    ```

2.  **Configure Environment Variables**
    Create a `.env` file in the root directory:
    ```properties
    MAIL_USERNAME=your_email@gmail.com
    MAIL_PASSWORD=your_app_password
    GEMINI_API_KEY=your_gemini_api_key
    ```

3.  **Run with Docker Compose**
    ```sh
    docker-compose up -d --build
    ```

4.  **Access the Application**
    *   **Frontend:** http://localhost:3000
    *   **Backend API:** http://localhost:8080
    *   **RabbitMQ UI:** http://localhost:15672 (guest/guest)
    *   **Grafana:** http://localhost:3001 (admin/admin)

## 👨‍💻 Author

**[Quang Khai]**

*   **Role:** Full-stack Developer
*   **Email:** [quangkhai251004@gmail.com]


