import base64, json, hmac, hashlib, time, subprocess, threading
URL='https://it-production-385a.up.railway.app'
secret='1234'
exp=int(time.time())+3600

def jwt_token(sub, role):
    header = {"alg":"HS256","typ":"JWT"}
    payload = {"sub": sub, "role": role, "exp": exp}
    def b64u(b):
        return base64.urlsafe_b64encode(b).decode().rstrip('=')
    header_b = b64u(json.dumps(header, separators=(',',':')).encode())
    payload_b = b64u(json.dumps(payload, separators=(',',':')).encode())
    to_sign = f"{header_b}.{payload_b}".encode()
    sig = hmac.new(secret.encode(), to_sign, hashlib.sha256).digest()
    sig_b = base64.urlsafe_b64encode(sig).decode().rstrip('=')
    return f"{header_b}.{payload_b}.{sig_b}"

client = jwt_token('client@example.com','CLIENT')
cleaner1 = jwt_token('cleaner1@example.com','CLEANER')
cleaner2 = jwt_token('cleaner2@example.com','CLEANER')
print('TOKENS:')
print('CLIENT', client)
print('CLEANER1', cleaner1)
print('CLEANER2', cleaner2)

# Test unauthenticated endpoints
print('\nGET /')
print(subprocess.run(['curl','-s','-S','-D','-','-o','-', URL+'/'], capture_output=True, text=True).stdout)
print('\nGET /actuator/health')
print(subprocess.run(['curl','-s','-S','-D','-','-o','-', URL+'/actuator/health'], capture_output=True, text=True).stdout)

# Create a job with client token
print('\nPOST /api/jobs (create)')
proc = subprocess.run(['curl','-s','-S','-H', f'Authorization: Bearer {client}', '-H','Content-Type: application/json','-X','POST','-d','{"description":"test from script"}', URL+'/api/jobs'], capture_output=True, text=True)
print('STDOUT:', proc.stdout)
print('STDERR:', proc.stderr)
if proc.returncode!=0:
    print('curl failed with', proc.returncode)
    raise SystemExit(1)

# parse body for id
body = proc.stdout.strip()
try:
    data = json.loads(body)
    job_id = data.get('id')
    print('created job id', job_id)
except Exception as e:
    print('Could not parse create response as JSON:', e)
    raise SystemExit(0)

# list open jobs as cleaner1
print('\nGET /api/jobs/open as CLEANER1')
proc2 = subprocess.run(['curl','-s','-S','-H', f'Authorization: Bearer {cleaner1}', URL+'/api/jobs/open'], capture_output=True, text=True)
print(proc2.stdout)

# concurrent accept
print('\nStarting concurrent accept attempts...')
res1 = {}
res2 = {}
def do_accept(token, out):
    p = subprocess.run(['curl','-s','-S','-H', f'Authorization: Bearer {token}', '-w','HTTP_CODE:%{http_code}','-X','POST', f"{URL}/api/jobs/{job_id}/accept"], capture_output=True, text=True)
    out['stdout']=p.stdout
    out['stderr']=p.stderr
    out['rc']=p.returncode

threads = []
for token, out in [(cleaner1,res1),(cleaner2,res2)]:
    t = threading.Thread(target=do_accept, args=(token,out))
    threads.append(t)
    t.start()
for t in threads:
    t.join()

print('\nRESULT CLEANER1:')
print(res1)
print('\nRESULT CLEANER2:')
print(res2)

