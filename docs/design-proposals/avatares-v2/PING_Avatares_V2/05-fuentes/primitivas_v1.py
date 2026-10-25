def path(d, fill=INK, stroke=None, w=4):
    return f'<path d="{d}" fill="{fill}"' + (f' stroke="{stroke}" stroke-width="{w}" stroke-linejoin="round" stroke-linecap="round"' if stroke else '') + '/>'

def ellipse(x, y, rx, ry, fill, stroke=None, w=4):
    return f'<ellipse cx="{x}" cy="{y}" rx="{rx}" ry="{ry}" fill="{fill}"' + (f' stroke="{stroke}" stroke-width="{w}"' if stroke else '') + '/>'

def group(name, body):
    return f'<g id="{name}" inkscape:groupmode="layer" inkscape:label="{name}">{body}</g>'

def eyes():
    body = ''
    for x in [87, 169]:
        body += path(f'M {x - 16} 148 C {x - 12} 129 {x + 11} 128 {x + 18} 146 C {x + 11} 161 {x - 9} 164 {x - 16} 148 Z', '#FFFDF5', INK, 3.5)
        body += ellipse(x + 3, 146, 9.5, 13, '#70453B') + ellipse(x + 5, 146, 5.8, 9.5, INK) + ellipse(x + 1, 140, 3.2, 3.2, '#FFFDF5')
    return body

def mouth(rabbit=False):
    nose = path('M 117 171 Q 128 166 139 171 Q 140 175 129 182 Q 126 183 117 176 Q 113 173 117 171 Z', INK)
    if rabbit:
        return nose + path('M 128 182 L 128 187 M 101 185 Q 107 197 128 195 Q 150 198 157 185', 'none', INK, 3.5) + path('M 120 195 L 120 206 Q 128 211 136 206 L 136 195 Z', '#FFFFFF', INK, 2.5)
    return nose + path('M 128 182 L 128 187 M 104 186 Q 115 201 128 187 Q 141 200 153 184', 'none', INK, 3.5)

def base(kind):
    ears = ''
    head = ''
    mask = ''
    details = ''
    if kind == 'panda-rojo':
        ears = path('M 49 102 C 24 78 23 46 35 27 C 50 23 73 44 85 68 Z', CREAM, INK) + path('M 171 68 C 185 43 205 25 220 31 C 230 51 228 81 210 105 Z', CREAM, INK)
        ears += path('M 44 81 Q 32 52 40 43 Q 57 46 70 68 Z', '#B45343') + path('M 188 68 Q 204 44 215 46 Q 222 63 209 88 Z', '#B45343')
        head = path('M 128 65 C 91 60 62 75 49 101 Q 43 119 34 130 L 42 137 L 31 151 L 44 154 L 34 168 L 48 171 Q 47 207 82 218 Q 128 237 175 218 Q 209 208 209 175 L 222 171 L 213 156 L 226 152 L 214 138 L 221 130 Q 207 115 206 101 C 194 75 164 62 128 65 Z', '#DC7345', INK)
        mask += path('M 40 153 Q 46 125 66 125 Q 83 127 91 143 Q 74 159 76 180 Q 76 195 92 204 Q 55 204 44 176 L 49 168 Z', CREAM)
        mask += path('M 216 154 Q 210 127 192 125 Q 175 127 165 143 Q 184 160 181 181 Q 180 197 164 204 Q 200 203 211 176 L 207 168 Z', CREAM)
        mask += path('M 78 177 C 84 153 108 156 128 164 C 148 156 176 155 182 177 Q 191 205 158 216 Q 128 228 96 215 Q 69 205 78 177 Z', CREAM)
        details += path('M 72 117 Q 76 100 89 101 Q 98 104 99 116 Q 86 110 72 117 Z', CREAM) + path('M 157 115 Q 158 101 170 101 Q 181 101 186 118 Q 170 110 157 115 Z', CREAM)
        details += path('M 56 202 Q 85 220 128 221 Q 172 219 200 200 Q 191 220 156 227 Q 112 237 77 219 Z', '#C3623F')
    elif kind == 'nutria':
        ears = ellipse(44, 116, 23, 28, '#A86E51', INK) + ellipse(211, 116, 23, 28, '#A86E51', INK) + ellipse(44, 117, 11, 15, '#765043') + ellipse(211, 117, 11, 15, '#765043')
        head = path('M 128 74 C 81 71 47 98 44 141 Q 35 181 63 205 Q 89 227 128 226 Q 170 227 194 206 Q 222 181 213 141 C 209 99 177 73 128 74 Z', '#B77C57', INK)
        mask = path('M 51 161 Q 62 147 83 153 Q 104 162 128 164 Q 154 160 175 153 Q 197 148 208 163 Q 215 196 179 214 Q 129 238 77 215 Q 43 199 51 161 Z', CREAM)
        details = path('M 69 122 Q 81 109 97 119 M 157 119 Q 171 109 184 122', 'none', INK, 4.5)
        details += path('M 47 173 L 67 178 M 44 185 L 65 185 M 190 179 L 210 174 M 192 187 L 214 188', 'none', '#7C5144', 2.5)
    elif kind == 'gato':
        ears = path('M 52 109 Q 29 63 41 26 Q 58 21 95 76 Z', '#B09DCB', INK) + path('M 162 76 Q 197 23 216 28 Q 226 67 208 111 Z', '#B09DCB', INK)
        ears += path('M 54 88 Q 43 55 48 43 Q 68 47 82 79 Z', '#EDACAD') + path('M 179 80 Q 196 48 208 45 Q 215 64 201 92 Z', '#EDACAD')
        head = path('M 128 72 Q 177 65 201 106 Q 212 127 211 151 L 222 163 L 209 165 L 220 180 L 204 181 Q 200 209 171 221 Q 128 235 86 221 Q 54 207 51 181 L 36 180 L 47 165 L 35 162 L 48 150 Q 44 121 55 105 Q 79 69 128 72 Z', '#B7A6D1', INK)
        mask = path('M 128 91 L 112 124 Q 111 145 93 158 Q 77 165 73 184 Q 70 209 103 221 Q 128 230 158 219 Q 185 208 184 184 Q 179 164 162 158 Q 143 145 142 125 Z', CREAM)
        details = path('M 71 122 Q 85 111 99 120 M 155 120 Q 170 110 184 121', 'none', INK, 4.5)
        details += path('M 48 174 L 73 180 M 46 190 L 73 191 M 183 180 L 209 174 M 184 191 L 213 190', 'none', '#7C6C97', 2.5)
        details += path('M 117 87 L 122 107 L 127 91 L 132 110 L 138 87 Z', '#E4DAED')
    else:
        ears = path('M 81 124 C 51 104 33 57 45 18 Q 49 8 60 15 C 88 35 106 80 104 116 Z', CREAM, INK) + path('M 152 117 C 149 75 173 29 199 17 Q 210 14 211 27 C 215 62 190 108 174 123 Z', CREAM, INK)
        ears += path('M 78 100 Q 52 67 53 30 Q 80 45 92 98 Z', '#EDA3A3') + path('M 166 100 Q 169 59 201 32 Q 203 66 177 106 Z', '#EDA3A3')
        head = path('M 128 105 Q 168 102 191 132 Q 207 151 204 175 L 216 184 L 205 187 Q 198 213 167 225 Q 129 237 88 224 Q 57 213 51 188 L 40 185 L 52 175 Q 48 151 66 131 Q 88 104 128 105 Z', CREAM, INK)
        mask = path('M 61 185 Q 77 206 128 209 Q 177 207 196 186 Q 192 216 155 228 Q 118 236 87 222 Q 66 210 61 185 Z', '#F4D5BB')
        details = path('M 72 127 Q 86 116 99 126 M 155 126 Q 169 116 183 127', 'none', INK, 4.5)
        details += ellipse(74, 181, 9, 4, '#F1B7AE') + ellipse(181, 181, 9, 4, '#F1B7AE')
    return group('01-orejas', ears) + group('02-cabeza', head) + group('03-manchas-y-volumen', mask) + group('04-cejas-y-detalles', details) + group('05-ojos', eyes()) + group('06-hocico', mouth(kind == 'conejo'))