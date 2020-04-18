FROM nginx
LABEL version=1

EXPOSE 25577

COPY nginx.conf /etc/nginx/nginx.conf

CMD ["nginx", "-g", "daemon off;"]