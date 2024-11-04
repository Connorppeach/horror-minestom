f = open("profanity_en.csv", "r")
lines = f.readlines()

for i in lines:
    sp = i.split(",")
    if(" " in sp[0]):
        pass
    print(i, end='')
    
