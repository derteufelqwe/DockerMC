from flask import Flask, request

app = Flask(__name__)

ILLEGAL_FILES = ['Webserver.py', 'requirements.txt']

@app.route('/get')
def getFile():
    filename = request.args.get('file')

    if filename == None:
        return 'NO_FILENAME'

    if filename in ILLEGAL_FILES:
        return 'ILLEGAL_FILE'

    try:
        with open('data/' + filename, 'r') as file:
            return str(file.read())

    except FileNotFoundError:
        return 'FILE_NOT_FOUND'


if __name__ == '__main__':
    print('[Info] Starting webserver')
    app.run(host='0.0.0.0', port=8101, debug=False)
