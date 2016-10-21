// This is a generated file! Please edit source .ksy file and use kaitai-struct-compiler to rebuild

package io.kaitai.struct.formats;

import io.kaitai.struct.KaitaiStruct;
import io.kaitai.struct.KaitaiStream;

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Wmf extends KaitaiStruct {
    public static Wmf fromFile(String fileName) throws IOException {
        return new Wmf(new KaitaiStream(fileName));
    }

    public enum Func {
        EOF(0),
        SETWINDOWORG(523),
        CREATEPENINDIRECT(762),
        CREATEPALETTE(247),
        ANIMATEPALETTE(1078),
        INTERSECTCLIPRECT(1046),
        SELECTOBJECT(301),
        SETPIXEL(1055),
        TEXTOUT(1313),
        SETRELABS(261),
        SCALEWINDOWEXT(1040),
        RECTANGLE(1051),
        OFFSETWINDOWORG(527),
        ROUNDRECT(1564),
        OFFSETVIEWPORTORG(529),
        SCALEVIEWPORTEXT(1042),
        SETTEXTALIGN(302),
        SETROP2(260),
        SETMAPPERFLAGS(561),
        SETLAYOUT(329),
        SETTEXTJUSTIFICATION(522),
        REALIZEPALETTE(53),
        SETVIEWPORTEXT(526),
        CREATEREGION(1791),
        STRETCHBLT(2851),
        POLYLINE(805),
        DIBBITBLT(2368),
        BITBLT(2338),
        INVERTREGION(298),
        SETBKCOLOR(513),
        SETTEXTCHAREXTRA(264),
        SETMAPMODE(259),
        RESIZEPALETTE(313),
        POLYGON(804),
        CREATEBRUSHINDIRECT(764),
        ESCAPE(1574),
        ELLIPSE(1048),
        EXTTEXTOUT(2610),
        FILLREGION(552),
        SETTEXTCOLOR(521),
        PAINTREGION(299),
        CREATEPATTERNBRUSH(505),
        CHORD(2096),
        LINETO(531),
        FLOODFILL(1049),
        SETVIEWPORTORG(525),
        RESTOREDC(295),
        SETSTRETCHBLTMODE(263),
        POLYPOLYGON(1336),
        OFFSETCLIPRGN(544),
        SETWINDOWEXT(524),
        ARC(2071),
        SETBKMODE(258),
        SETPALENTRIES(55),
        CREATEFONTINDIRECT(763),
        EXCLUDECLIPRECT(1045),
        SETDIBTODEV(3379),
        SELECTCLIPREGION(300),
        STRETCHDIB(3907),
        SETPOLYFILLMODE(262),
        SAVEDC(30),
        SELECTPALETTE(564),
        MOVETO(532),
        FRAMEREGION(1065),
        EXTFLOODFILL(1352),
        DIBCREATEPATTERNBRUSH(322),
        DIBSTRETCHBLT(2881),
        PIE(2074),
        PATBLT(1565),
        DELETEOBJECT(496);

        private final long id;
        Func(long id) { this.id = id; }
        public long id() { return id; }
        private static final Map<Long, Func> byId = new HashMap<Long, Func>(70);
        static {
            for (Func e : Func.values())
                byId.put(e.id(), e);
        }
        public static Func byId(long id) { return byId.get(id); }
    }

    public Wmf(KaitaiStream _io) throws IOException {
        super(_io);
        this._root = this;
        _parse();
    }

    public Wmf(KaitaiStream _io, KaitaiStruct _parent) throws IOException {
        super(_io);
        this._parent = _parent;
        this._root = this;
        _parse();
    }

    public Wmf(KaitaiStream _io, KaitaiStruct _parent, Wmf _root) throws IOException {
        super(_io);
        this._parent = _parent;
        this._root = _root;
        _parse();
    }
    private void _parse() throws IOException {
        this.specialHeader = new SpecialHeader(this._io, this, _root);
        this.header = new Header(this._io, this, _root);
        this.records = new ArrayList<Record>();
        {
            Record _it;
            do {
                _it = new Record(this._io, this, _root);
                this.records.add(_it);
            } while (!(_it.function() == Func.EOF));
        }
    }
    public static class SpecialHeader extends KaitaiStruct {
        public static SpecialHeader fromFile(String fileName) throws IOException {
            return new SpecialHeader(new KaitaiStream(fileName));
        }

        public SpecialHeader(KaitaiStream _io) throws IOException {
            super(_io);
            _parse();
        }

        public SpecialHeader(KaitaiStream _io, Wmf _parent) throws IOException {
            super(_io);
            this._parent = _parent;
            _parse();
        }

        public SpecialHeader(KaitaiStream _io, Wmf _parent, Wmf _root) throws IOException {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _parse();
        }
        private void _parse() throws IOException {
            this.magic = this._io.ensureFixedContents(4, new byte[] { -41, -51, -58, -102 });
            this.handle = this._io.ensureFixedContents(2, new byte[] { 0, 0 });
            this.left = this._io.readS2le();
            this.top = this._io.readS2le();
            this.right = this._io.readS2le();
            this.bottom = this._io.readS2le();
            this.inch = this._io.readU2le();
            this.reserved = this._io.ensureFixedContents(4, new byte[] { 0, 0, 0, 0 });
            this.checksum = this._io.readU2le();
        }
        private byte[] magic;
        private byte[] handle;
        private short left;
        private short top;
        private short right;
        private short bottom;
        private int inch;
        private byte[] reserved;
        private int checksum;
        private Wmf _root;
        private Wmf _parent;
        public byte[] magic() { return magic; }
        public byte[] handle() { return handle; }
        public short left() { return left; }
        public short top() { return top; }
        public short right() { return right; }
        public short bottom() { return bottom; }
        public int inch() { return inch; }
        public byte[] reserved() { return reserved; }
        public int checksum() { return checksum; }
        public Wmf _root() { return _root; }
        public Wmf _parent() { return _parent; }
    }
    public static class Header extends KaitaiStruct {
        public static Header fromFile(String fileName) throws IOException {
            return new Header(new KaitaiStream(fileName));
        }

        public enum MetafileType {
            MEMORY_METAFILE(1),
            DISK_METAFILE(2);

            private final long id;
            MetafileType(long id) { this.id = id; }
            public long id() { return id; }
            private static final Map<Long, MetafileType> byId = new HashMap<Long, MetafileType>(2);
            static {
                for (MetafileType e : MetafileType.values())
                    byId.put(e.id(), e);
            }
            public static MetafileType byId(long id) { return byId.get(id); }
        }

        public Header(KaitaiStream _io) throws IOException {
            super(_io);
            _parse();
        }

        public Header(KaitaiStream _io, Wmf _parent) throws IOException {
            super(_io);
            this._parent = _parent;
            _parse();
        }

        public Header(KaitaiStream _io, Wmf _parent, Wmf _root) throws IOException {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _parse();
        }
        private void _parse() throws IOException {
            this.metafileType = MetafileType.byId(this._io.readU2le());
            this.headerSize = this._io.readU2le();
            this.version = this._io.readU2le();
            this.size = this._io.readU4le();
            this.numberOfObjects = this._io.readU2le();
            this.maxRecord = this._io.readU4le();
            this.numberOfMembers = this._io.readU2le();
        }
        private MetafileType metafileType;
        private int headerSize;
        private int version;
        private long size;
        private int numberOfObjects;
        private long maxRecord;
        private int numberOfMembers;
        private Wmf _root;
        private Wmf _parent;
        public MetafileType metafileType() { return metafileType; }
        public int headerSize() { return headerSize; }
        public int version() { return version; }
        public long size() { return size; }
        public int numberOfObjects() { return numberOfObjects; }
        public long maxRecord() { return maxRecord; }
        public int numberOfMembers() { return numberOfMembers; }
        public Wmf _root() { return _root; }
        public Wmf _parent() { return _parent; }
    }
    public static class Record extends KaitaiStruct {
        public static Record fromFile(String fileName) throws IOException {
            return new Record(new KaitaiStream(fileName));
        }

        public Record(KaitaiStream _io) throws IOException {
            super(_io);
            _parse();
        }

        public Record(KaitaiStream _io, Wmf _parent) throws IOException {
            super(_io);
            this._parent = _parent;
            _parse();
        }

        public Record(KaitaiStream _io, Wmf _parent, Wmf _root) throws IOException {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _parse();
        }
        private void _parse() throws IOException {
            this.size = this._io.readU4le();
            this.function = Func.byId(this._io.readU2le());
            this.params = this._io.readBytes(((size() - 3) * 2));
        }
        private long size;
        private Func function;
        private byte[] params;
        private Wmf _root;
        private Wmf _parent;
        public long size() { return size; }
        public Func function() { return function; }
        public byte[] params() { return params; }
        public Wmf _root() { return _root; }
        public Wmf _parent() { return _parent; }
    }
    private SpecialHeader specialHeader;
    private Header header;
    private ArrayList<Record> records;
    private Wmf _root;
    private KaitaiStruct _parent;
    public SpecialHeader specialHeader() { return specialHeader; }
    public Header header() { return header; }
    public ArrayList<Record> records() { return records; }
    public Wmf _root() { return _root; }
    public KaitaiStruct _parent() { return _parent; }
}
