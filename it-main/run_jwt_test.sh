#!/usr/bin/env bash
SECRET="1234"
EXP=$(($(date +%s) + 3600))
HEADER_B64=$(printf '%s' '{"alg":"HS256","typ":"JWT"}' | openssl base64 -e -A | tr '+/' '-_' | tr -d '=')
PAY_CLIENT_B64=$(printf '%s' "{\"sub\":\"client@example.com\",\"role\":\"CLIENT\",\"exp\":$EXP}" | openssl base64 -e -A | tr '+/' '-_' | tr -d '=')
SIG_CLIENT=$(printf '%s' "$HEADER_B64.$PAY_CLIENT_B64" | openssl dgst -binary -sha256 -hmac "$SECRET" | openssl base64 -e -A | tr '+/' '-_' | tr -d '=')
TOKEN_CLIENT="$HEADER_B64.$PAY_CLIENT_B64.$SIG_CLIENT"
PAY_C1_B64=$(printf '%s' "{\"sub\":\"cleaner1@example.com\",\"role\":\"CLEANER\",\"exp\":$EXP}" | openssl base64 -e -A | tr '+/' '-_' | tr -d '=')
SIG_C1=$(printf '%s' "$HEADER_B64.$PAY_C1_B64" | openssl dgst -binary -sha256 -hmac "$SECRET" | openssl base64 -e -A | tr '+/' '-_' | tr -d '=')
TOKEN_C1="$HEADER_B64.$PAY_C1_B64.$SIG_C1"
PAY_C2_B64=$(printf '%s' "{\"sub\":\"cleaner2@example.com\",\"role\":\"CLEANER\",\"exp\":$EXP}" | openssl base64 -e -A | tr '+/' '-_' | tr -d '=')
SIG_C2=$(printf '%s' "$HEADER_B64.$PAY_C2_B64" | openssl dgst -binary -sha256 -hmac "$SECRET" | openssl base64 -e -A | tr '+/' '-_' | tr -d '=')
TOKEN_C2="$HEADER_B64.$PAY_C2_B64.$SIG_C2"
echo "TOKENS GENERATED (truncated):"
echo "CLIENT: ${TOKEN_CLIENT:0:40}..."
echo "CLEANER1: ${TOKEN_C1:0:40}..."
echo "CLEANER2: ${TOKEN_C2:0:40}..."

CREATE_RESP=$(curl -s -X POST -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN_CLIENT" -d '{"description":"Prueba desde Railway - concurrent accept"}' https://it-production-8f68.up.railway.app/api/jobs)

echo "\nCREATE RESPONSE:"
echo "$CREATE_RESP"

JOB_ID=$(echo "$CREATE_RESP" | grep -o '"id"\s*:\s*[0-9]\+' | grep -o '[0-9]\+' | head -n1)

if [ -z "$JOB_ID" ]; then
  echo "\nCould not extract JOB_ID. Aborting concurrent accept test. Full create response above."
  exit 0
fi

echo "\nCreated job id: $JOB_ID"

echo "\nOPEN JOBS (as CLEANER1):"
OPEN=$(curl -s -H "Authorization: Bearer $TOKEN_C1" https://it-production-8f68.up.railway.app/api/jobs/open)
echo "$OPEN"

OUT1=$(mktemp)
OUT2=$(mktemp)

curl -s -w "\nHTTP_CODE:%{http_code}" -X POST -H "Authorization: Bearer $TOKEN_C1" https://it-production-8f68.up.railway.app/api/jobs/$JOB_ID/accept > "$OUT1" &
PID1=$!

curl -s -w "\nHTTP_CODE:%{http_code}" -X POST -H "Authorization: Bearer $TOKEN_C2" https://it-production-8f68.up.railway.app/api/jobs/$JOB_ID/accept > "$OUT2" &
PID2=$!

wait $PID1 $PID2

echo "\n---- ACCEPT RESULTS ----"
echo "CLEANER1 response:"; cat "$OUT1"; echo "\nCLEANER2 response:"; cat "$OUT2"

rm -f "$OUT1" "$OUT2"

