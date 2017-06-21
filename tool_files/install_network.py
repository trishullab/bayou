import urllib.request
import tarfile

localDir  = '../src/main/resources/network/'
localFile = localDir + 'popl18.tar.gz'

print('downloading network...')
urllib.request.urlretrieve('http://release.askbayou.com/popl18.tar.gz', localFile)

print('expanding network...')
tar = tarfile.open(localFile)
tar.extractall(path=localDir)
tar.close()
