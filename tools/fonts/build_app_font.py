"""Build OFL composite UI font: Inter Latin, SUIT Korean, Plex unit symbols.
Original fonts stay unchanged. Generated derivative has its own family name.
"""
from pathlib import Path
from tempfile import TemporaryDirectory
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools.ttLib.scaleUpem import scale_upem
from fontTools import subset
from fontTools.merge import Merger

ROOT = Path(__file__).resolve().parents[2]
SOURCES = Path(__file__).resolve().parent / 'sources'
OUT = ROOT / 'source/app/src/main/res/font'

def build(weight):
    with TemporaryDirectory() as temp:
        seen = set()
        files = []
        sources = [SOURCES/'inter-variable.ttf', SOURCES/'suit-variable.ttf',
                   SOURCES/f'ibm_plex_sans_kr_{ {500:"medium",600:"semibold",700:"bold"}[weight]}.ttf']
        for index, source in enumerate(sources):
            font = TTFont(source)
            if 'fvar' in font:
                axes = {a.axisTag: (weight if a.axisTag == 'wght' else a.defaultValue) for a in font['fvar'].axes}
                font = instantiateVariableFont(font, axes, inplace=True)
            available = set(font.getBestCmap())
            chosen = available - seen
            seen |= chosen
            options = subset.Options()
            options.layout_features = ['*']
            sub = subset.Subsetter(options=options)
            sub.populate(unicodes=chosen)
            sub.subset(font)
            scale_upem(font, 1000)
            target = Path(temp)/f'{index}.ttf'
            font.save(target)
            files.append(str(target))
        merged = Merger().merge(files)
        names = merged['name']
        for nid, value in [(1,'TD Remote UI'),(2,str(weight)),(3,f'TDRemoteUI-{weight}-1'),(4,f'TD Remote UI {weight}'),(6,f'TDRemoteUI-{weight}'),(16,'TD Remote UI'),(17,str(weight))]:
            names.removeNames(nameID=nid)
            names.setName(value,nid,3,1,0x409)
        merged['OS/2'].usWeightClass = weight
        merged.save(OUT/f'app_sans_{weight}.ttf')
        cmap=merged.getBestCmap()
        assert all(ord(c) in cmap for c in '한글ABC123㎃㎽㎷Ｖ㎲')
        print(weight, len(cmap), 'glyphs mapped')

if __name__ == '__main__':
    for weight in (500,600,700):
        build(weight)
