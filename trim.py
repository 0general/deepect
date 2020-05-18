import cv2
import numpy as np
import datetime



def trim(img):
    image = cv2.imread(img)
    img_gray = cv2.imread(img, 0)
    height, width = np.shape(img_gray)
    print(height, width)
    basename = "scp"
    suffix1 = datetime.datetime.now().strftime("%y%m%d_%H%M%S")
    suffix2 = '.jpg'
    filename = "_".join([basename, suffix1, suffix2])
    a, b, c, d = 810*height, 140*width, 600*height, 800*width
    y, x, h, w = int(a/2220), int(b/1080), int(c/2220), int(d/1080)
    scp = image[y:y+h, x:x+w]
    cv2.imwrite(filename, scp)
    trim_image = cv2.imread(filename)
    return trim_image

cv2.imshow('trimtest', trim('test2.jpg'))
cv2.waitKey(0)
cv2.destroyAllWindows()
