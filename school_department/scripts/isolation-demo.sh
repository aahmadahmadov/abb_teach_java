#!/usr/bin/env bash

BASE=${BASE:-http://localhost:8080/test-demo}
API=$BASE/api
DELAY=8000

echo "===== D1: Non-repeatable read (department 1) ====="
SALARY=6000

for ISO in READ_COMMITTED REPEATABLE_READ; do

  echo "--- $ISO"
  curl -s "$API/isolation/departments/1/salary-report?isolation=$ISO&delayMs=$DELAY" &

  sleep 2

  curl -s -o /dev/null -X PUT "$API/employees/2" -H "Content-Type: application/json" \
    -d "{\"name\":\"Vusal\",\"surname\":\"Aliyev\",\"salary\":$SALARY,\"departmentId\":1}"

  wait
  echo

  SALARY=$((SALARY + 1000))
done

echo
echo "===== D2: Lost update (employee 4, iki dəfə +100) ====="

for ISO in READ_COMMITTED REPEATABLE_READ; do

  echo "--- $ISO"

  curl -s "$API/employees/4" | grep -o '"salary":[0-9.]*' | sed 's/^/before /'

  curl -s -X POST "$API/isolation/employees/4/raise?amount=100&isolation=$ISO&delayMs=$DELAY" &
  curl -s -X POST "$API/isolation/employees/4/raise?amount=100&isolation=$ISO&delayMs=$DELAY" &

  wait
  echo

  curl -s "$API/employees/4" | grep -o '"salary":[0-9.]*' | sed 's/^/after /'
done

echo

echo "===== D3: Write skew (course 5, capacity = 1) ====="

for ISO in REPEATABLE_READ SERIALIZABLE; do

  echo "--- $ISO"

  curl -s -X POST "$API/isolation/courses/5/enroll/1?isolation=$ISO&delayMs=$DELAY" &
  curl -s -X POST "$API/isolation/courses/5/enroll/2?isolation=$ISO&delayMs=$DELAY" &

  wait
  echo
  echo "student count: $(curl -s "$API/courses/5/students/count")"

  curl -s -o /dev/null -X DELETE "$API/students/1/courses/5"
  curl -s -o /dev/null -X DELETE "$API/students/2/courses/5"
done
