# Stage 1: Build React App
FROM node:18-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Stage 2: Serve with Nginx
FROM nginx:alpine
# Copy file build từ stage 1 vào thư mục web của Nginx
COPY --from=build /app/dist /usr/share/nginx/html
# Copy file cấu hình Nginx custom (ta sẽ tạo ở bước sau)
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
