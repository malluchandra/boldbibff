mvn clean install
docker build -t mallucharan/boldbi-proxy:latest .

docker push mallucharan/boldbi-proxy:latest

docker-compose up# boldbibff