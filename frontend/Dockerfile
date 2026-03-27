FROM node:20-alpine AS builder
WORKDIR /workspace/frontend
COPY frontend/package.json ./package.json
COPY frontend/yarn.lock ./yarn.lock
RUN yarn install --frozen-lockfile
COPY frontend ./
COPY ukiyo-e-digital-style-pack ../ukiyo-e-digital-style-pack
RUN yarn build

FROM nginx:alpine
COPY --from=builder /workspace/frontend/dist /usr/share/nginx/html
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
