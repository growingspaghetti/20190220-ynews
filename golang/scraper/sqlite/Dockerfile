FROM tetafro/golang-gcc:1.14-alpine as builder
RUN mkdir /build
ADD . /build/
WORKDIR /build
RUN CGO_ENABLED=1 go build -o main .

FROM alpine:3.12.0
RUN addgroup --gid 1000 -S appgroup && adduser --uid 1000 -h /app -S appuser -G appgroup
COPY --from=builder /build/main /app/
RUN chown -R appuser:appgroup /app
USER appuser
WORKDIR /app
ENTRYPOINT ["./main"]
