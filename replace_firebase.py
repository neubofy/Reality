with open('website/src/lib/firebase.ts', 'r') as f:
    content = f.read()

content = content.replace("getRedirectResult,", "")

with open('website/src/lib/firebase.ts', 'w') as f:
    f.write(content)
