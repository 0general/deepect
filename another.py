import cv2 as cv
import numpy as np


# top left, top right, bottom right, bottom left
def sort_points(points):
    points = points.astype(np.float32)

    new_points = np.zeros((4, 2), dtype="float32")

    s = points.sum(axis=1)
    min_index = np.argmin(s)
    new_points[0] = points[min_index]
    points = np.delete(points, min_index, axis=0)

    s = points.sum(axis=1)
    max_index = np.argmax(s)
    new_points[2] = points[max_index]
    points = np.delete(points, max_index, axis=0)

    if points[0][1] > points[1][1]:
        new_points[1] = points[1]
        new_points[3] = points[0]
    else:
        new_points[1] = points[0]
        new_points[3] = points[1]

    return new_points


def transform(img_input, points):
    points = sort_points(points)
    topLeft, topRight, bottomRight, bottomLeft = points

    maxWidth = 800
    maxHeight = 300

    dst = np.array([[0, 0], [maxWidth - 1, 0],
                    [maxWidth - 1, maxHeight - 1], [0, maxHeight - 1]],
                   dtype="float32")

    H = cv.getPerspectiveTransform(points, dst)
    img_warped = cv.warpPerspective(img_input, H, (maxWidth, maxHeight))

    return img_warped


img_color = cv.imread('sign1.jpg', cv.IMREAD_COLOR)

height, width = img_color.shape[:2]
cv.imshow('@', img_color)
cv.waitKey(0)

img_gray = cv.cvtColor(img_color, cv.COLOR_BGR2GRAY)
cv.imshow('@', img_gray)
cv.waitKey(0)

# 이진화로 물체와 배경 분리
# 물체만 영상에 남긴다.
ret, img_binary = cv.threshold(img_gray, 0, 255,
                               cv.THRESH_BINARY | cv.THRESH_OTSU)
cv.imshow('@', img_binary)

# 잡음 제거
kernel = cv.getStructuringElement(cv.MORPH_RECT, (3, 3))
img_binary = cv.morphologyEx(img_binary, cv.MORPH_OPEN, kernel)
cv.imshow('@', img_binary)
cv.waitKey(0)

# findContour로 컨투어 검출
contours, hierarchy = cv.findContours(img_binary, cv.RETR_EXTERNAL,
                                      cv.CHAIN_APPROX_SIMPLE)

for contour in contours:  # 각 컨투어에 대해서

    area = cv.contourArea(contour)  # 면적을 구한다

    if area < 10000:  # 면적 100이하 제외.  면적 설정은 논의 필요
        continue

    epsilon = 0.02 * cv.arcLength(contour, True)
    approx = cv.approxPolyDP(contour, epsilon, True)

    size = len(approx)

    img_result = img_color.copy()
    cv.drawContours(img_result, [approx], -1, (0, 255, 0), 2);
    '''
    cv.imshow('@', img_result)
    cv.waitKey(0)
    '''

    if cv.isContourConvex(approx):
        if size == 4:  # 네개의 엣지를 가지면 사각형
            hull = cv.convexHull(approx)  # convexHull로 윤곽(껍질) 구하기

            points = []

            for p in hull:  # 포인트를 리스트에 입력
                points.append(p[0])
            points = np.array(points)

            img_card = transform(img_color, points)  # 정면으로 변화
            img_gray = cv.cvtColor(img_card, cv.COLOR_BGR2GRAY)

            max = -1
            max_idx = -1

            # 내가 추가한 코드
            cv.imshow('sign', img_card)
            cv.waitKey(0)
            '''
            #여기서부터는 확인 과정 
            for i in range(1,5): #미리 저장해둔 이미지를 불러와서 여기서는 4개 불러옴
                img_template = cv.imread( str(i) + '.jpg', cv.IMREAD_GRAYSCALE)

                #템플릿 매칭 시작  유사한 인덱스 확인
                res = cv.matchTemplate(img_gray, img_template, cv.TM_CCOEFF)
                min_val, max_val, min_loc, max_loc = cv.minMaxLoc(res)

                if max < max_val:
                    max = max_val
                    max_idx = i
                #템플릿 매칭 종료

            img_template = cv.imread( str(max_idx) + '.jpg', cv.IMREAD_GRAYSCALE)
            img_card = cv.hconcat([img_gray, img_template])

            cv.imshow('Card', img_card)
            cv.waitKey(0)
            '''