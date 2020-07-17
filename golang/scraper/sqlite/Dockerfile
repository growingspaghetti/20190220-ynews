FROM tetafro/golang-gcc:1.14-alpine as builder
RUN mkdir /build
ADD . /build/
WORKDIR /build
RUN CGO_ENABLED=1 go build -o main .

FROM alpine:3.12.0
RUN addgroup -S appgroup && adduser -h /app -S appuser -G appgroup
USER appuser
COPY --from=builder /build/main /app/
WORKDIR /app
ENTRYPOINT ["./main"]
