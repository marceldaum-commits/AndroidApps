#!/usr/bin/env python3
"""Erzeugt die Quelltexte der JavaScript-Fassung der WebApp aus denen der Wasm-Fassung.

Die Wasm-Fassung bettet Browser-Code so ein:   fun name(a: A, b: B): T = js("…modernes JavaScript…")
Kotlin/JS versteht in js("…") nur altes JavaScript. Darum wird jede solche Funktion umgeschrieben: Der JavaScript-Text wird zur Laufzeit
einmal zu einer Funktion gemacht (new Function) und dann aufgerufen. Alles andere bleibt Zeichen für Zeichen gleich – es gibt nur eine Quelle.

Aufruf:  gen.py <Quellordner> <Zielordner>
"""
import os, re, sys, shutil

HEAD = re.compile(r'((?:(?:private|internal|public)\s+)?fun\s+(\w+)\s*\()')

def params_end(s, i):
    depth = 1
    while depth:
        c = s[i]
        if c == '(': depth += 1
        elif c == ')': depth -= 1
        i += 1
    return i                                    # Stelle nach der schließenden Klammer

def split_params(p):
    out, depth, cur = [], 0, ''
    for c in p:
        if c in '(<': depth += 1
        if c in ')>': depth -= 1
        if c == ',' and depth == 0: out.append(cur); cur = ''
        else: cur += c
    if cur.strip(): out.append(cur)
    return [x.strip().split(':')[0].strip() for x in out]

def unescape(s):
    return re.sub(r'\\(.)', lambda m: {'n': '\n', 't': '\t', '"': '"', "'": "'", '\\': '\\', '$': '$'}[m.group(1)], s)

def convert(src):
    out, pos, n = [], 0, 0
    for m in HEAD.finditer(src):
        if m.start() < pos: continue
        end = params_end(src, m.end())
        tail = re.match(r'\s*(?::\s*([^={\n]+?))?\s*=\s*js\(\s*', src[end:])
        if not tail: continue
        start = end + tail.end()
        if src.startswith('"""', start):
            close = src.index('"""', start + 3); code = src[start + 3:close]; after = close + 3
        else:
            lit = re.match(r'"((?:[^"\\]|\\.)*)"', src[start:]); code = unescape(lit.group(1)); after = start + lit.end()
        assert src[after] == ')', (m.group(2), src[after:after + 20])
        names = split_params(src[m.end():end - 1]); typ = (tail.group(1) or 'Unit').strip(); name = m.group(2)
        body = code.strip()
        body = body if body.startswith('{') else 'return (' + body + ');'
        assert '"""' not in body
        raw = body.replace('$', "${'$'}")
        call = '__js_%s(%s)' % (name, ', '.join(names))
        out.append(src[pos:m.start()])
        out.append('private val __js_%s: dynamic = __jsFunction("%s", """%s""")\n' % (name, ','.join(names), raw))
        out.append(src[m.start():end] + (': ' + typ if tail.group(1) else '') + (' { %s }' % call if typ == 'Unit' else ' = %s.unsafeCast<%s>()' % (call, typ)))
        pos = after + 1; n += 1
    out.append(src[pos:])
    return ''.join(out), n

def main(src_dir, dst_dir):
    shutil.rmtree(dst_dir, ignore_errors=True); total = 0; first = True
    for root, _, files in os.walk(src_dir):
        for f in sorted(files):
            if not f.endswith('.kt'): continue
            p = os.path.join(root, f); text, n = convert(open(p, encoding='utf-8').read()); total += n
            assert 'js("' not in text.replace('__jsFunction("', ''), 'nicht umgesetzter js()-Aufruf in ' + p
            q = os.path.join(dst_dir, os.path.relpath(p, src_dir)); os.makedirs(os.path.dirname(q), exist_ok=True)
            if n:
                pkg = re.search(r'^package\s+([\w.]+)', text, re.M).group(1)
                helper = os.path.join(os.path.dirname(q), '__JsFunction.kt')
                if not os.path.exists(helper):
                    open(helper, 'w', encoding='utf-8').write('package %s\n\n/** Macht aus JavaScript-Text eine Funktion (nur in der JavaScript-Fassung der WebApp; erzeugt von tools/webjs/gen.py) */\n'
                        'internal fun __jsFunction(params: String, body: String): dynamic = js("new Function(params, body)")\n' % pkg)
            open(q, 'w', encoding='utf-8').write('// ERZEUGT aus %s – nicht von Hand ändern (tools/webjs/gen.py)\n' % os.path.relpath(p, src_dir) + text)
    print('JavaScript-Fassung: %d eingebettete Browser-Funktionen umgesetzt' % total)

if __name__ == '__main__': main(sys.argv[1], sys.argv[2])
