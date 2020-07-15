import sys

#create i-th 4KB memory file filled with input bytes array
#each of data lines has binary value of data_width size (2^4)
#consecutive even and odd files would form 32bits data
#there are total of data_depth=size/2 lines (2^11)
#total of stored bits is 2^4*2^11 = 2^15 = 32Kbits = 4KB
def create_dat_file(filename, bytes, i, size=4096, f='016b'):
  with open(filename + "_" + str(i%2) + "_" + str(i//2) + ".dat", "w") as newFile:
    for j in range(size//2):
      k = 2*size*(i//2)+2*(i%2)+4*j
      c = bytes[k]+256*bytes[k+1]
      newFile.write(format(c, f) + "\n")

#create 4KB memory mif file filled with zeros (not used at the moment)
def create_mif_file(filename, bytes, i):
  with open(filename + "_" + str(i%2) + "_" + str(i//2) + ".mif", "w") as newFile:
    newFile.write("DEPTH = 2048;\n")
    newFile.write("WIDTH = 16;\n")
    newFile.write("ADDRESS_RADIX = UNS;\n")
    newFile.write("DATA_RADIX = BIN;\n")
    newFile.write("CONTENT BEGIN\n")
    for j in range(2048):
      newFile.write(str(j) + " : 0000000000000000;\n")
    newFile.write("END;\n")

def main(argv):
  file = "bram16x32k.bin"
  bytes = None
  if len(argv) > 1:
    file = argv[1]
  with open(file, "rb") as binaryfile:
    bytes = bytearray(binaryfile.read())
  for i in range(16):
    create_dat_file(file, bytes, i)

if __name__=="__main__":
  main(sys.argv)

#To run tests: pytest split.py
def test_create_dat_file():
  result = []
  for i in range(4):
    create_dat_file("test.bin", bytearray(range(32)), i, 8, '04x')
    file = "test.bin" + "_" + str(i%2) + "_" + str(i//2) + ".dat"
    with open(file) as f:
      read_data = f.read()
      result.append(read_data)
  assert result[0] == '0100\n0504\n0908\n0d0c\n'
  assert result[1] == '0302\n0706\n0b0a\n0f0e\n'
  assert result[2] == '1110\n1514\n1918\n1d1c\n'
  assert result[3] == '1312\n1716\n1b1a\n1f1e\n'
