function Init_API() {
    return {
        urlNode: 'https://api-apis.com',
        DonVi: "CMCCongThongTin",
        CM: '/csacmapi/api',
        SYS: '/csacmapi/api',
        CMS: '/cmsapi/api',
        DKH: '/dangkyhocapi/api',
        KHCT: '/kehoachchuongtrinhapi/api',
        KS: '/qlkhaosatapi/api',
        KTX: '/kytucxaapi/api',
        NCKH: '/nckhapi/api',
        NS: '/nhansuapi/api',
        D: '/quanlydiemapi/api',
        SV: '/sinhvienapi/api',
        SMS: '/smsapi/api',
        TC: '/taichinhapi/api',
        TKGG: '/thongkegiogiangapi/api',
        L: '/luongapi/api',
        CC: '/chuyencanapi/api',
        HLTL: '/hoclaithilaiapi/api',
        RL: '/renluyenapi/api',
        XLHV: '/xulyhocvuapi/api',
        HDDT: '/hddtmisaapi/api',
        NH: '/nhaphocapi/api',
        TS: '/quanlytuyensinhapi/api',
        LVLA: '/luanvanluananapi/api',
        QLTTN: '/quanlythitracnghiemapi/api',
        HB: '/hocbongapi/api',
	TP: '/thiphachapi/api',
        TT: '/tintucapi/api',
        TN: '/totnghiepapi/api',
	urlKhaoSat: '/congthongtin/pages/thuchienkhaosat.aspx',
        checkUser: 'Yes',
	checkCanBo: 'true',
    };
}
//Replace html & js before load. warning 
function ReplicaWithText() {
    return {
        All: {

        },
        ApisCongCanBo: {

        }
    };
}
//replace html after load
function ReplicaWithDom() {
    return {
        All: {
            ".cosoxet": "trường."//
        },
        ApisCongCanBo: {
        }
    };
}
