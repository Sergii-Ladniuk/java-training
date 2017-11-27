#!/usr/bin/env bash
curl -H 'Content-Type: application/json' -X POST http://localhost:7000/api/v1/categories -d '{"name":"soft drinks"}' | jq
curl -H 'Content-Type: application/json' -X POST http://localhost:7000/api/v1/categories -d '{"name":"tea"}' | jq
curl -H 'Content-Type: application/json' -X POST http://localhost:7000/api/v1/items -d '{"name":"black tea", "price":10.3, "category":{"id":2,"name":"tea"}}'