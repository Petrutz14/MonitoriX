FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
ARG BACKEND_URL=http://localhost:8080
ARG WS_URL=ws://localhost:8080/ws
RUN sed -i "s|__BACKEND_URL__|${BACKEND_URL}|g; s|__WS_URL__|${WS_URL}|g" src/environments/environment.prod.ts
RUN npm run build -- --configuration=production

FROM nginx:alpine
COPY --from=build /app/dist/MonitorX_Frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
