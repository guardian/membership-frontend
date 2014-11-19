import sys, os, base64
import xml.etree.ElementTree as ET

prefix = sys.argv[1]

for fn in sys.stdin:
    fn = fn.strip() # strip trailing line break

    name, _ = os.path.splitext(os.path.basename(fn))
    rule_name = '%s-%s' % (prefix, name)

    etree = ET.parse(fn).getroot()
    width = etree.attrib['width']
    height = etree.attrib['height']

    data = 'data:image/svg+xml;base64,' + base64.b64encode(open(fn).read())

    print '%%%s, .%s { display: inline-block; width: %spx; height: %spx; background-image: url(%s); }' % (rule_name, rule_name, width, height, data)
