import cv2
import datetime


def trim(img):
    image = cv2.imread(img)
    basename = "scp"
    suffix1 = datetime.datetime.now().strftime("%y%m%d_%H%M%S")
    suffix2 = '.jpg'
    filename = "_".join([basename, suffix1, suffix2])
    y, x, h, w = 100,200,150,300
    scp = image[y:y+h, x:x+w]
    cv2.imwrite(filename, scp)
    trim_image = cv2.imread(filename)
    return trim_image
'''
cv2.imshow('trimtest', trim('sign1.jpg'))
cv2.waitKey(0)
cv2.destroyAllWindows()
'''