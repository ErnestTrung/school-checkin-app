/*----------------------------------------------
--Author: 
--Phone:
--Date of created: 29/06/2018
--Input:
--Output:
--Note:
----------------------------------------------*/
function LichGiang() { };
LichGiang.prototype = {
    strThongTin_Id: '',
    dtThongTin: [],
    iNgayBatDau : 0,
    strGiangVien_Id: '',
    arrDay: [],
    dtLichHoc: [],
    dtTKBKhongLichChiTiet: [],
    strLichHoc_Id: '',
    dtKieuChuyenCan: [],
    dtSinhVien: [],
    strNgayBatDau: '',
    strNgayKetThuc: '',
    strlblHocPhan: '',
    init: function () {
        var me = this;
        /*------------------------------------------
        --Discription: Initial system 
        -------------------------------------------*/
        me.strGiangVien_Id = edu.system.userId;
        me.getList_CamXuc();
        //me.getList_ThongTin();
        var date = new Date();
        var nMonth = date.getMonth() + 1;
        var nYear = date.getFullYear();

        $("#nam").attr("title", nYear);
        $("#nam").html(nYear);

        $("#thang").attr("title", nMonth);
        $("#thang").html("Tháng " + nMonth);
        me.genHtml_Month(0);

        me.bindCalendarScrollSync();

        $(".days").delegate(".poiter", "click", function () {
            $(".days .active").removeClass("active");
            $("#datebody").html("");
            this.classList.add("active");
            var strNgayBatDau = $(this).attr('batdau');
            var strNgayKetThuc = $(this).attr('ketthuc');
            var strNgayDangChon = $(this).attr('title');
            me.strNgayBatDau = strNgayBatDau;
            me.strNgayKetThuc = strNgayKetThuc;
            me.getList_TuanHienTai(strNgayBatDau, strNgayKetThuc, strNgayDangChon);
            me.getList_TKBLopKhongCoLichChiTiet(strNgayDangChon);
            var strClass = $(this).attr('name');
            strClass = $("." + strClass);
            var html = '';
            html += '<div class="date" title="Giờ Việt Nam (GMT+7)">';
            html += '<i class="fal fa-clock day color-66"></i>';
            html += '<p class="m-0 text">Giờ VN</p>';
            html += '</div>';

            var arrDayName = ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'Chủ nhật'];
            var todayObj = new Date();
            var todayStr = ('0' + todayObj.getDate()).slice(-2) + '/' + ('0' + (todayObj.getMonth() + 1)).slice(-2) + '/' + todayObj.getFullYear();
            for (var iDay = 0; iDay < 7; iDay++) {
                var dayTitle = $(strClass[iDay]).attr('title') || '';
                var dayNum = dayTitle.split('/')[0] || '';
                var isToday = dayTitle === todayStr;
                html += '<div class="day-of-week' + (isToday ? ' today-col' : '') + '" title="' + dayTitle + '">';
                html += '<div class="day">' + dayNum + '</div>';
                html += '<div class="text">' + arrDayName[iDay] + '</div>';
                html += '</div>';
            }

            $("#date-header").html(html);
            me.arrDay = [];
            for (var i = 0; i < strClass.length; i++) {
                if (i > 6) break;
                me.arrDay.push($(strClass[i]).attr('title').replace(/\//g, ''));
            }
        });
        $(".month").delegate(".prev", "click", function () {
            me.genHtml_Month(-1);
        });
        $(".month").delegate(".next", "click", function () {
            me.genHtml_Month(1);
        });

        $("#tblThongTin").delegate(".btnXacNhan", "click", function () {
            var strId = this.id;
            edu.system.confirm("Bạn có đồng ý xác nhận thời điểm vào lớp không?");
            $("#btnYes").click(function (e) {
                me.save_XacNhan(strId);
            });
        });
        $("#tblThongTin").delegate(".btnXacNhan", "click", function () {
            var strId = this.id;
            edu.system.confirm("Bạn có đồng ý xác nhận thời điểm vào lớp không?");
            $("#btnYes").click(function (e) {
                me.save_XacNhan(strId);
            });
        });

        $("#datebody").delegate(".btnLichHoc", "click", function () {
            var strId = this.id;
            me.strLichHoc_Id = strId;
            $("#my_calendar").modal("show");
            var objLich = me.dtLichHoc.find(e => e.ID === strId);
            $("#lblLopHocPhan").html('<b>Lớp: ' + objLich.TENLOPHOCPHAN + '</b> <span class="color-66 fs-14"> (Phòng học: ' + objLich.TENPHONGHOC + ')</span>');
            $("#lblHocPhan").html('<b>Học phần: ' + objLich.TENHOCPHAN + '</b> <span class="color-66">&ensp; - &ensp; ' + me.returnTwo(objLich.GIOBATDAU) + ':' + me.returnTwo(objLich.PHUTBATDAU) + ' - ' + me.returnTwo(objLich.GIOKETTHUC) + ':' + me.returnTwo(objLich.PHUTKETTHUC) + ' (Tiết ' + edu.util.returnEmpty(objLich.TIETBATDAU) + '-' + edu.util.returnEmpty(objLich.TIETKETTHUC) + ') &ensp;&ensp; <b>' + edu.util.returnEmpty(objLich.THONGTINCHUYENCAN) + '</b></span>');
            me.strlblHocPhan = $("#lblHocPhan").html();
            me.getList_SinhVien(strId);
        });
        $("#datebody").delegate(".btnLichThi", "click", function () {
            var strId = this.id;
            me.strLichHoc_Id = strId;
            $("#modal_LichThi").modal("show");
            var objLich = me.dtLichHoc.find(e => e.ID === strId);
            $("#lblHocPhanThi").html(objLich.TENHOCPHAN);
            $("#lblHinhThucThi").html(objLich.DANGKY_LOPHOCPHAN_TEN);
            $("#lblPhongThi").html(objLich.PHONGHOC_TEN);
            $("#lblNgayThi").html(objLich.NGAYHOC);
            $("#lblGioThi").html(objLich.GIOBATDAU + "h" + objLich.PHUTBATDAU + " - " + objLich.GIOKETTHUC + "h" + objLich.PHUTKETTHUC);
            me.getList_LichThi(objLich.NGAYHOC);
        });

        $("#datebody").delegate('.task-1', 'mouseenter', function (e) {
            e.stopImmediatePropagation();
            var point = this;
            //console.log(point.clientHeight)
            //console.log(point.scrollHeight)
            let element = $(point).find(".task-description")[0];
            //console.log($(point).find(".task-description")[0].clientHeight)
            //console.log($(point).find(".task-description")[0].scrollHeight)
            if (element.scrollHeight <= element.clientHeight) return;
            var row = $(point).parent().html().replace('class="task-header"', '');
            row = row.substring(0, row.indexOf('<div class="eval"'))
            $(point).popover({
                container: 'body',
                content: row,
                trigger: 'hover',
                html: true,
                placement: 'right',
            });
            $(point).popover('show');
            //me.popover_HSDoiTuong(id, point);
        });
        $("#datebody").delegate(".eval", "click", function (event) {
            event.stopImmediatePropagation();
        });
        $("#datebody").delegate(".btnEmoChange", "click", function (event) {
            event.stopImmediatePropagation();
            var strId = this.parentNode.parentNode.parentNode.parentNode.parentNode.id;
            var strEmo = $(this).attr("name");
            me.save_ThayDoiCamXuc(strId, strEmo)
        });
        $("#datebody").delegate(".btnEmoPlus", "click", function (event) {
            event.stopImmediatePropagation();
            var strId = this.parentNode.parentNode.parentNode.id;
            var strEmo = $("#" + strId + " #mainicon").attr("name");
            me.save_TangCamXuc(strId, strEmo)
        });
        $("#datebody").delegate(".btnEmoMinus", "click", function (event) {
            event.stopImmediatePropagation();
            var strId = this.parentNode.parentNode.parentNode.id;
            var strEmo = $("#" + strId + " #mainicon").attr("name");
            me.save_GiamCamXuc(strId, strEmo)
        });

        //edu.system.loadToCombo_DanhMucDuLieu("QLSV.KIEUCHUYENCAN", "", "", data => {
        //    me.dtKieuChuyenCan = data;
        //    var row = '';
        //    data.forEach(e => {
        //        row += '<th class="td-center">' + e.TEN + ' <br/> <input type="checkbox" class="chkSelectAll" id="chkSelectAll_' + e.ID + '"></th>';
        //    });
            
        //    $("#tblNhapChuyenCan thead tr:eq(0)").append(row);
        //});
        $("#tblNhapChuyenCan").delegate(".chkSelectAll", "click", function (e) {

            var checked_status = this.checked;
            var strClass = this.id.substring(this.id.indexOf("_") + 1);
            $(".checkNgay" + strClass).each(function () {
                this.checked = checked_status;
            });
        });

        $("#btnSaveChuyenCan").click(function () {
            me.save_TuGhiNhan();
            //var arrThem = [];
            //var arrXoa = [];
            //var x = $("#tblNhapChuyenCan .checkChuyenCan");
            //for (var i = 0; i < x.length; i++) {
            //    if ($(x[i]).is(':checked')) {
            //        if ($(x[i]).attr("name") == undefined) {
            //            arrThem.push(x[i]);
            //        } else {
            //            var inputcheck = $("#" + x[i].id.replace("chkSelect", "input_"));
            //            if (inputcheck.attr("name") != inputcheck.val()) arrThem.push(x[i]);
            //        }
            //    }
            //    else {
            //        if ($(x[i]).attr("name") != undefined) {
            //            arrXoa.push(x[i]);
            //        }
            //    }
            //}
            //if ((arrThem.length + arrXoa.length) > 0) {
            //    //edu.system.confirm("Bạn có chắc chắn lưu " + arrThem.length + " và hủy " + arrXoa.length + "?");
            //    //$("#btnYes").click(function (e) {
                    
            //    //});
            //    //edu.system.genHTML_Progress("divprogessdata", arrThem.length + arrXoa.length);
            //    for (var i = 0; i < arrThem.length; i++) {
            //        me.save_NhapChuyenCan(arrThem[i]);
            //    }
            //    for (var i = 0; i < arrXoa.length; i++) {
            //        me.delete_NhapChuyenCan(arrXoa[i]);
            //    }
            //}
            //else {
            //    //edu.system.alert("Không có thay đổi lưu");
            //}
        });

        $("#btnDiemDanhTuDong").click(function () {
            me.save_DiemDanhTuDong();
        });
        edu.system.getList_MauImport("zonebtnBaoCao_LichGiang", function (addKeyValue) {
            var obj_save = {
                'strNhanSu_HoSoCanBo_Id': main_doc.LichGiang.strGiangVien_Id,
                'strNgayBatDau': main_doc.LichGiang.strNgayBatDau,
                'strNgayKetThuc': main_doc.LichGiang.strNgayKetThuc,
            };
            for (variable in obj_save) {
                addKeyValue(variable, obj_save[variable]);
            }
        });
    },

    bindCalendarScrollSync: function () {
        var header = document.getElementById('date-header');
        var body = document.getElementById('datebody');
        if (!header || !body) return;
        if (header.__scrollSyncBound || body.__scrollSyncBound) return;

        var syncing = false;
        $(body).on('scroll', function () {
            if (syncing) return;
            syncing = true;
            header.scrollLeft = body.scrollLeft;
            syncing = false;
        });
        $(header).on('scroll', function () {
            if (syncing) return;
            syncing = true;
            body.scrollLeft = header.scrollLeft;
            syncing = false;
        });

        header.__scrollSyncBound = true;
        body.__scrollSyncBound = true;

        this.bindDragScroll(body);
        this.bindDragScroll(header);
    },

    bindDragScroll: function (el) {
        if (!el || el.__dragScrollBound) return;
        el.__dragScrollBound = true;

        var isDown = false, startX = 0, startY = 0, moved = false;
        var xTarget = null, yTarget = null, xStart = 0, yStart = 0;

        el.style.cursor = 'grab';

        function findScrollable(node, axis) {
            while (node && node !== document.body && node !== document.documentElement) {
                var style = window.getComputedStyle(node);
                var ov = axis === 'x' ? style.overflowX : style.overflowY;
                var canScroll = (ov === 'auto' || ov === 'scroll' || ov === 'overlay');
                var dim = axis === 'x' ? (node.scrollWidth - node.clientWidth) : (node.scrollHeight - node.clientHeight);
                if (canScroll && dim > 1) return node;
                node = node.parentElement;
            }
            return null;
        }

        function getScroll(target, axis) {
            if (!target) return window['scroll' + (axis === 'x' ? 'X' : 'Y')] || (document.documentElement['scroll' + (axis === 'x' ? 'Left' : 'Top')]);
            return axis === 'x' ? target.scrollLeft : target.scrollTop;
        }

        function setScroll(target, axis, val) {
            if (!target) {
                if (axis === 'x') window.scrollTo({ left: val, top: window.scrollY, behavior: 'instant' });
                else window.scrollTo({ left: window.scrollX, top: val, behavior: 'instant' });
                return;
            }
            if (axis === 'x') target.scrollLeft = val;
            else target.scrollTop = val;
        }

        el.addEventListener('mousedown', function (e) {
            if (e.button !== 0) return;
            if (e.target.closest('.task, .btnLichHoc, .btnLichThi, .eval, .btnEmoChange, .btnEmoPlus, .btnEmoMinus, a, button, input, select, textarea')) return;
            isDown = true;
            moved = false;
            startX = e.pageX;
            startY = e.pageY;

            xTarget = findScrollable(el, 'x') || findScrollable(el.parentElement, 'x');
            yTarget = findScrollable(el, 'y') || findScrollable(el.parentElement, 'y');

            xStart = xTarget ? xTarget.scrollLeft : (window.scrollX || document.documentElement.scrollLeft);
            yStart = yTarget ? yTarget.scrollTop : (window.scrollY || document.documentElement.scrollTop);

            el.style.cursor = 'grabbing';
            document.body.style.userSelect = 'none';
            e.preventDefault();
        });

    
        var stop = function () {
            if (!isDown) return;
            isDown = false;
            el.style.cursor = 'grab';
            document.body.style.userSelect = '';
        };

        document.addEventListener('mouseup', stop);

        document.addEventListener('mousemove', function (e) {
            if (!isDown) return;
            var dx = e.pageX - startX;
            var dy = e.pageY - startY;
            if (Math.abs(dx) > 3 || Math.abs(dy) > 3) moved = true;

            if (xTarget) {
                xTarget.scrollLeft = xStart - dx;
            } else {
                window.scrollTo({ left: xStart - dx, top: window.scrollY, behavior: 'instant' });
            }

            if (yTarget) {
                yTarget.scrollTop = yStart - dy;
            } else if (Math.abs(dy) > 0) {
                window.scrollTo({ left: window.scrollX, top: yStart - dy, behavior: 'instant' });
            }
        });

        el.addEventListener('click', function (e) {
            if (moved) {
                e.stopPropagation();
                e.preventDefault();
                moved = false;
            }
        }, true);
    },

    getList_TKBLopKhongCoLichChiTiet: function (strNgay) {
        var me = this;
        if (!strNgay) {
            me.dtTKBKhongLichChiTiet = [];
            me.renderTable_TKBLopKhongCoLichChiTiet([]);
            return;
        }

        var obj_save = {
            'action': 'SV_ThongTin_MH/DSA4FQoDDS4xCikuLyYCLg0oIikCKSgVKCQ1',
            'func': 'PKG_CONGTHONGTIN_HSSV_THONGTIN.LayTKBLopKhongCoLichChiTiet',
            'iM': edu.system.iM,
            'strQLSV_NguoiHoc_Id': me.strGiangVien_Id,
            'strNgay': strNgay,
            'strChucNang_Id': edu.system.strChucNang_Id,
            'strNguoiThucHien_Id': edu.system.userId,
        };

        edu.system.makeRequest({
            success: function (data) {
                if (data.Success) {
                    me.dtTKBKhongLichChiTiet = data.Data || [];
                    me.renderTable_TKBLopKhongCoLichChiTiet(me.dtTKBKhongLichChiTiet);
                }
                else {
                    me.dtTKBKhongLichChiTiet = [];
                    me.renderTable_TKBLopKhongCoLichChiTiet([]);
                }
            },
            error: function (er) {
                me.dtTKBKhongLichChiTiet = [];
                me.renderTable_TKBLopKhongCoLichChiTiet([]);
            },
            type: 'POST',
            action: obj_save.action,
            contentType: true,
            data: obj_save,
            fakedb: []
        }, false, false, false, null);
    },

    renderTable_TKBLopKhongCoLichChiTiet: function (data) {
        var me = this;
        var $zone = $("#zoneTKBKhongLichChiTiet");
        var $tbody = $("#tblTKBKhongLichChiTiet tbody");
        if ($tbody.length === 0) return;

        if (!data || data.length === 0) {
            $zone.show();
            $tbody.html(''
                + '<tr class="empty-state-row">'
                + '<td colspan="6" style="text-align:center; padding:36px 16px; border:none;">'
                + '<div style="display:inline-block; padding:18px 28px; background:#fafbff; border:1px dashed #d9deeb; border-radius:14px; color:#7a8499;">'
                + '<i class="fal fa-users-class" style="font-size:42px; color:#b8c0d4; display:block; margin-bottom:8px;"></i>'
                + '<div style="font-size:14px;">Hiện tại chưa có lớp học phần (không có lịch chi tiết) nào</div>'
                + '</div>'
                + '</td>'
                + '</tr>');
            return;
        }

        $zone.show();
        var html = '';
        for (var i = 0; i < data.length; i++) {
            var a = data[i] || {};
            html += '<tr>';
            html += '<td>' + edu.util.returnEmpty(a.MALOP) + '</td>';
            html += '<td>' + edu.util.returnEmpty(a.TENLOP) + '</td>';
            html += '<td>' + edu.util.returnEmpty(a.TENHINHTHUCHOC) + '</td>';
            html += '<td>' + edu.util.returnEmpty(a.NGAYBATDAU) + '</td>';
            html += '<td>' + edu.util.returnEmpty(a.NGAYKETTHUC) + '</td>';
            html += '<td>' + edu.util.returnEmpty(a.GHICHU) + '</td>';
            html += '</tr>';
        }
        $tbody.html(html);
    },
    /*------------------------------------------
    --Discription: [3] AccessDB HOC
    --ULR:  Modules
    -------------------------------------------*/
    getList_TuanHienTai: function (strNgayBatDau, strNgayKetThuc) {
        var me = this;
        //--Edit
        var obj_save = {
            'action': 'SV_ThongTin_MH/DSA4BRINKCIpAiAPKSAv',
            'func': 'pkg_congthongtin_hssv_thongtin.LayDSLichCaNhan',
            'iM': edu.system.iM,
            'strQLSV_NguoiHoc_Id': me.strGiangVien_Id,
            'strNgayBatDau': strNgayBatDau,
            'strNgayKetThuc': strNgayKetThuc,
        };
        //

        edu.system.makeRequest({
            success: function (data) {
                if (data.Success) {
                    var dtReRult = data.Data;
                    //if (dtReRult.length > 0) {
                    //    var aData = dtReRult[0];
                    //    $("#lblNgayHienTai").html("Thứ " + edu.util.returnEmpty(aData.THU) + ", ngày " + edu.util.returnEmpty(aData.NGAY));
                    //}
                    me.dtLichHoc = dtReRult;
                    me.genTable_ThongTin(dtReRult, data.Pager);
                }
                else {
                    edu.system.alert(data.Message, "s");
                }

            },
            error: function (er) {

                edu.system.alert(JSON.stringify(er), "w");
            },
            type: 'POST',
            action: obj_save.action,

            contentType: true,
            data: obj_save,
            fakedb: [

            ]
        }, false, false, false, null);
    },
    /*------------------------------------------
    --Discription: [3] AccessDB HOC
    --ULR:  Modules
    -------------------------------------------*/
    getList_SinhVien: function (strDanhSach_Id) {
        var me = this;
        //--Edit
        var objLich = me.dtLichHoc.find(e => e.ID === me.strLichHoc_Id);

        var obj_save = {
            'action': 'NS_ThongTinCanBo_MH/DSA4BRIFIC8mCjgJLiIecwPP',
            'func': 'pkg_congthongtincanbo.LayDSDangKyHoc_2',
            'iM': edu.system.iM,
            'strTuKhoa': edu.util.getValById('txtAAAA'),
            'strNgayGhiNhan': objLich.NGAYHOC,
            'dGio': objLich.GIOBATDAU,
            'dPhut': objLich.PHUTBATDAU,
            'dGiay': 0,
            'strReport_Id': edu.util.getValById('dropAAAA'),
            'strDaoTao_LopHocPhan_Id': objLich.IDLOPHOCPHAN,
            'strNguoiThucHien_Id': edu.system.userId,
        };
        //

        edu.system.makeRequest({
            success: function (data) {
                if (data.Success) {
                    var dtReRult = data.Data;
                    me.dtSinhVien = dtReRult;
                    me.genTable_SinhVien(dtReRult, data.Pager);
                    console.log(dtReRult.find(e => e.QLSV_NGUOIHOC_ID == me.strGiangVien_Id));
                    var aData = dtReRult.find(e => e.QLSV_NGUOIHOC_ID == me.strGiangVien_Id).SOBUOIVANG;
                    console.log(aData)
                    if (aData) {
                        $("#lblHocPhan").html(me.strlblHocPhan + '<br/> Vắng mặt(Số buổi/Số tiết/Tỷ lệ): <b>' + aData + '</b>')
                    }
                }
                else {
                    edu.system.alert(data.Message, "s");
                }

            },
            error: function (er) {

                edu.system.alert(JSON.stringify(er), "w");
            },
            type: 'POST',
            action: obj_save.action,

            contentType: true,
            data: obj_save,
            fakedb: [

            ]
        }, false, false, false, null);
    },
    genTable_SinhVien: function (data, iPager) {
        var me = this;
        var objSinhVien = data.find(e => e.QLSV_NGUOIHOC_ID == me.strGiangVien_Id);
        if (objSinhVien) {
            $("#txtTuKhoaDiemDanh").val(objSinhVien.MATLENHNGUOIHOC);
        } else
            $("#txtTuKhoaDiemDanh").val("");
        var jsonForm = {
            strTable_Id: "tblNhapChuyenCan",
            
            aaData: data,
            colPos: {
                center: [0],
            },
            aoColumns: [
                {
                    "mDataProp": "QLSV_NGUOIHOC_MASO"
                },
                {
                    "mDataProp": "QLSV_NGUOIHOC_HODEM",
                },
                {
                    "mDataProp": "QLSV_NGUOIHOC_TEN"
                }
            ]
        };
        //for (var i = 0; i < me.dtKieuChuyenCan.length; i++) {
        //    jsonForm.aoColumns.push({
        //        "mRender": function (nRow, aData) {
        //            var iThuTu = edu.system.icolumn++;
        //            return '<input type="checkbox" class="checkChuyenCan checkSV' + aData.ID + ' checkNgay' + main_doc.LichGiang.dtKieuChuyenCan[iThuTu].ID + '" id="chkSelect' + aData.ID + '_' + main_doc.LichGiang.dtKieuChuyenCan[iThuTu].ID+ '"  />'
        //                + '<input style="width: calc(100% - 100px); float: right" id="input_' + aData.ID + '_' + main_doc.LichGiang.dtKieuChuyenCan[iThuTu].ID + '" />';
        //        }
        //    });
        //    jsonForm.colPos.center.push(jsonForm.aoColumns.length);
        //}
        
        edu.system.loadToTable_data(jsonForm);
        //edu.system.genHTML_Progress("divprogessquanso", data.rs.length * data.rsNgay.length);
        //me.getData_NhapChuyenCan();
    },
    getData_NhapChuyenCan: function (jsonSV, strNgay_ID, strNgay) {
        var me = this;
        var objLich = me.dtLichHoc.find(e => e.ID === me.strLichHoc_Id);
        //--Edit
        var obj_save = {
            'action': 'XLHV_CC_ThongTin_MH/DSA4CiQ1EDQgFSkkLgooJDQCKTQ4JC8CIC8P',
            'func': 'PKG_CHUYENCAN_THONGTIN.LayKetQuaTheoKieuChuyenCan',
            'iM': edu.system.iM,
            'strKieuChuyenCan_Id': edu.util.getValById('dropAAAA'),
            'strNgayGhiNhan': objLich.NGAYHOC,
            'strGio': objLich.GIOBATDAU,
            'strPhut': objLich.PHUTBATDAU,
            'strGiay': 0,
            'strQLSV_NguoiHoc_Id': edu.util.getValById('dropAAAA'),
            'strDaoTao_LopHocPhan_Id': objLich.IDLOPHOCPHAN,
        };
        //

        edu.system.makeRequest({
            success: function (data) {
                if (data.Success) {
                    var dtReRult = data.Data;
                    for (var i = 0; i < dtReRult.length; i++) {
                        if (dtReRult[i].GIATRI == 1) {
                            var check = $("#chkSelect" + dtReRult[i].QLSV_NGUOIHOC_ID + "_" + dtReRult[i].KIEUCHUYENCAN_ID);
                            check.attr('checked', true);
                            check.prop('checked', true);
                            check.attr('name', dtReRult[i].GIATRI);
                            var inputCheck = $("#input_" + dtReRult[i].QLSV_NGUOIHOC_ID + "_" + dtReRult[i].KIEUCHUYENCAN_ID);
                            inputCheck.val(dtReRult[i].SOLUONG);
                            inputCheck.attr("name", dtReRult[i].SOLUONG);
                        }
                    }
                }
                else {
                    edu.system.alert(data.Message, "s");
                }

                //edu.system.start_Progress("divprogessquanso", me.endGetData);
            },
            error: function (er) {

                //edu.system.alert(JSON.stringify(er), "w");
                //edu.system.start_Progress("divprogessquanso", me.endGetData);
            },
            type: 'POST',
            action: obj_save.action,

            contentType: true,
            data: obj_save,
            fakedb: [

            ]
        }, false, false, false, null);
    },
    /*------------------------------------------
    --Discription: [4] GenHTML Tiến độ đề tài
    --ULR:  Modules
    -------------------------------------------*/
    genTable_ThongTin: function (data, iPager) {
        var me = this;
        if (!data || data.length === 0) {
            $("#datebody").html(''
                + '<div style="width:100%; padding:48px 16px; text-align:center;">'
                + '<div style="display:inline-block; padding:24px 36px; background:#fafbff; border:1px dashed #d9deeb; border-radius:14px; color:#7a8499;">'
                + '<i class="fal fa-calendar-week" style="font-size:48px; color:#b8c0d4; display:block; margin-bottom:10px;"></i>'
                + '<div style="font-size:14px;">Hiện tại chưa có lịch học cá nhân nào trong tuần này</div>'
                + '</div>'
                + '</div>');
            return;
        }
        var iGioMin = 24;
        var iGioMax = 0;
        data.forEach(e => {
            if (e.GIOBATDAU < iGioMin) iGioMin = e.GIOBATDAU;
            if (e.GIOKETTHUC > iGioMax) iGioMax = e.GIOKETTHUC;
        });
        var html = '<div class="date">';
        for (var i = iGioMin; i < iGioMax + 2; i++) {
            html += '<div class="hour-row">' + i +':00</div>';
        }
        html += '</div>';
        for (var j = 0; j < me.arrDay.length; j++) {
            html += '<div class="day-of-week" id="row' + me.arrDay[j] +'">';
            for (var i = iGioMin; i < iGioMax + 2; i++) {
                html += '<div class="hour-row" id="h' + me.arrDay[j] + i +'"></div>';
            }
            html += '</div>';
        }
        $("#datebody").html(html);

        var arrMau = ["#223771", "#d49f3a", "#ec4c00", "#5a7adb", "#3c5398"];
        var arrLopHocPhanMau = [];
        data.forEach(e => {
            if (arrLopHocPhanMau.find(ele => ele.ID == e.IDLOPHOCPHAN) == undefined) {
                arrLopHocPhanMau.push({
                    ID: e.IDLOPHOCPHAN,
                    MAU: arrMau[arrLopHocPhanMau.length % arrMau.length]
                })
            }
        });
        data.forEach((e, nRow) => {
            var strMauNen = "";
            try {
                strMauNen = arrLopHocPhanMau.find(ele => ele.ID == e.IDLOPHOCPHAN).MAU;
            } catch{

            }
            var html = '';
            var iTop = e.GIOKETTHUC * 60 + e.PHUTKETTHUC - e.GIOBATDAU*60 - e.PHUTBATDAU;
            var temp = e.PHANLOAI == "LICHTHI" ? "btnLichThi" : "btnLichHoc";
            
            html += '<div class="task task-1 ' + temp + '" id="' + e.ID + '" style="top:' + (30 + e.PHUTBATDAU) + 'px; height: ' + iTop + 'px; background-color: ' + strMauNen + '; cursor: pointer">';
            //html += '<div class="client">';
            html += '<div class="task-header">';
            html += '<div class="text">';
            temp = e.PHANLOAI == "LICHTHI" ? "Lịch thi: "+ e.TENHOCPHAN : e.TENHOCPHAN;
            html += '<div class="title">' + temp + '</div>';
            temp = e.PHANLOAI == "LICHTHI" ? edu.util.returnEmpty(e.CATHI) : 'Tiết ' + edu.util.returnEmpty(e.TIETBATDAU) + '-' + edu.util.returnEmpty(e.TIETKETTHUC);
            html += '<div class="task-date">' + me.returnTwo(e.GIOBATDAU) + ':' + me.returnTwo(e.PHUTBATDAU) + ' - ' + me.returnTwo(e.GIOKETTHUC) + ':' + me.returnTwo(e.PHUTKETTHUC) + ' (' + temp +')</div>';
            html += '</div>';
            //html += '<div class="title">' + e.TENHOCPHAN + '</div>';
            //html += '<div class="task-date">' + e.TIETBATDAU + ' - ' + e.TIETKETTHUC + '</div>';
            //html += '</div>';
            html += '</div>';
            html += '<div class="task-description">';
            html += e.TENLOPHOCPHAN ? e.TENLOPHOCPHAN + "<br/>" : "";
            html += e.TENPHONGHOC ? edu.util.returnEmpty(e.TENPHONGHOC) + "<br/>" : "";
            html += e.GIANGVIEN ? edu.util.returnEmpty(e.GIANGVIEN) : "";
            html += '<div class="eval">';
            html += '<i class="eval-minus fal fa-minus btnEmoMinus"></i>';
            html += '<div class="eval-icon">';
            html += '<span id="lblSoLuong' + e.ID +'"></span>';
            html += '<i id="mainicon" class="fas fa-thumbs-up" style="color:#1877f2; font-size:14px; filter: drop-shadow(0 1px 2px rgba(24,119,242,0.4)); transition: transform 0.2s ease;" onmouseover="this.style.transform=\'scale(1.25)\'" onmouseout="this.style.transform=\'scale(1)\'"></i>';
            //html += '<div class="eval-icon-list" ><div class="item btnEmoChange" name="4AA5E457B3F74AD1A1A7F7310B41544F"><img src="assets/images/eval-emoji/happy.png.png" alt=""><span>undefined</span></div><div class="item btnEmoChange" name="6A36858F671A43CEAE6DDDAEDF443A31"><img src="assets/images/eval-emoji/neutral.png.png" alt=""><span>undefined</span></div><div class="item btnEmoChange" name="3FC3A490EB374C3EAFE9E512050171B6"><img src="assets/images/eval-emoji/unhappy.png.png" alt=""><span>undefined</span></div>';
            html += '<div class="eval-icon-list" id="listIcon' + e.ID + '">';
            html += me.strCamXuc;
            //html += '<div class="item">';
            //html += '<img src="assets/images/eval-emoji/1.png" alt="">';
            //html += '<span>5</span>';
            //html += '</div>';
            html += '</div>';

            html += '</div>';
            html += '<i class="eval-plus fal fa-plus btnEmoPlus"></i>';
            html += '</div>';

            html += '</div>';
            //html += '</div>';
            html += '</div>';
            $("#datebody #h" + e.NGAYHOC.replace(/\//g, '') + e.GIOBATDAU).append(html);
        });
        data.forEach(e => {
            me.getList_CamXucMacDinh(e.ID, e.IDLICHHOC);
            //me.getList_CamXuc(e.ID, e.IDLICHHOC);
        })
    },

    save_XacNhan: function (strId) {
        var me = this;
        var obj_notify = {};
        var obj_save = {
            'action': 'SV_LopHoc_Lich_SV/XacNhan_HoTro_LopHoc_Lich_SV',
            'type': 'POST',
            'strId': strId,
            'strNguoiThucHien_Id': edu.system.userId,
        };
        //default
        edu.system.makeRequest({
            success: function (data) {
                if (data.Success) {
                    edu.system.alert("Thực hiện thành công!");
                }
                else {
                    obj_notify = {
                        type: "w",
                        content: obj_save.action + " (er): " + data.Message,
                    }
                    edu.system.alert(data.Message);
                }
            },
            error: function (er) {
                edu.system.alertOnModal(obj_notify);
            },
            type: "POST",
            action: obj_save.action,
            contentType: true,
            data: obj_save,
            fakedb: [
            ]
        }, false, false, false, null);
    },

    genHtml_Month: function (cal) {
        var nMonth = parseInt($("#thang").attr("title"));
        var nYear = parseInt($("#nam").attr("title"));
        nMonth += cal;
        if (nMonth == 0) {
            nMonth = 12;
            nYear--;
            $("#nam").attr("title", nYear);
            $("#nam").html(nYear);
        }
        if (nMonth == 13) {
            nMonth = 1;
            nYear++;
            $("#nam").attr("title", nYear);
            $("#nam").html(nYear);
        }
        $("#thang").attr("title", nMonth);
        $("#thang").html("Tháng " + nMonth);

        var iDayOfMonth = getDay(nMonth, nYear);

        var strDay = edu.util.dateToday();
        var iDay = parseInt(strDay.substr(0, strDay.indexOf("/")));

        var iThu = new Date(nYear, nMonth - 1, 1).getDay();
        if (iThu == 0) iThu = 7;
        var iPadStart = iThu - 1;

        var iPreMonth = nMonth - 1, iPreYear = nYear;
        if (iPreMonth == 0) { iPreMonth = 12; iPreYear--; }
        var iDayOfPreMonth = getDay(iPreMonth, iPreYear);

        var iNextMonth = nMonth + 1, iNextYear = nYear;
        if (iNextMonth == 13) { iNextMonth = 1; iNextYear++; }

        var date = new Date();
        var bIsCurrentMonth = (date.getMonth() + 1 == nMonth && date.getFullYear() == nYear);
        var iToday = bIsCurrentMonth ? date.getDate() : -1;
        if (!bIsCurrentMonth) iDay = 1;

        var iTotalCells = Math.ceil((iPadStart + iDayOfMonth) / 7) * 7;
        var html = "";
        var uuid = edu.util.uuid();
        var strNgayBatDau = '';
        var strNgayKetThuc = '';

        function cellInfo(c) {
            if (c < iPadStart) {
                return { day: iDayOfPreMonth - (iPadStart - 1 - c), m: iPreMonth, y: iPreYear, isCur: false };
            } else if (c < iPadStart + iDayOfMonth) {
                return { day: c - iPadStart + 1, m: nMonth, y: nYear, isCur: true };
            } else {
                return { day: c - iPadStart - iDayOfMonth + 1, m: iNextMonth, y: iNextYear, isCur: false };
            }
        }

        for (var c = 0; c < iTotalCells; c++) {
            if (c % 7 == 0) {
                uuid = edu.util.uuid();
                var s = cellInfo(c);
                var e = cellInfo(c + 6);
                strNgayBatDau = getSMonth(s.day, s.m, s.y);
                strNgayKetThuc = getSMonth(e.day, e.m, e.y);
            }
            var info = cellInfo(c);
            if (!info.isCur) {
                html += '<li class="day-of-other-month ' + uuid + '" title="' + getSMonth(info.day, info.m, info.y) + '">' + info.day + '</li>';
            } else {
                var strClass = (info.day == iDay) ? 'active' : '';
                if (info.day == iToday) strClass += ' today';
                html += '<li class="poiter ' + strClass + ' ' + uuid + '" ngay="' + info.day + '"  title="' + getSMonth(info.day, info.m, info.y) + '" name="' + uuid + '" batdau="' + strNgayBatDau + '" ketthuc="' + strNgayKetThuc + '"><span>' + info.day + '</span></li>';
            }
        }
        $(".days").html(html);
        setTimeout(function () {
            $(".days .active").trigger("click");
        }, 1000);

        function getDay(nMonth, nYear) {
            var iDayOfMonth = 31;
            switch (nMonth) {
                case 2: {
                    iDayOfMonth = 28;
                    if (nYear % 4 == 0) iDayOfMonth = 29;
                } break;
                case 4:
                case 6:
                case 9:
                case 11: iDayOfMonth = 30; break;
            }
            return iDayOfMonth;
        }
        function getSMonth(iDay, nMonth, Year) {
            if (nMonth == 0) {
                nMonth = 12;
                Year--;
            }
            if (nMonth == 13) {
                nMonth = 1;
                Year++;
            }
            return returnTwo(iDay) + '/' + returnTwo(nMonth) + '/' + Year;
        }

        function returnTwo(iDay) {
            iDay = "" + iDay;
            if (iDay.length == 1) return "0" + iDay;
            else return iDay;
        }
    },
    returnTwo: function(iDay) {
        iDay = "" + iDay;
        if (iDay.length == 1) return "0" + iDay;
        else return iDay;
    },

    save_NhapChuyenCan: function (point) {
        var me = this;
        var strid = point.id;
        var arrId = strid.split("_");
        var strSV_Id = arrId[0].substring(9);
        var objSV = edu.util.objGetOneDataInData(strSV_Id, me.dtSinhVien, "ID");
        var strNgay = $(point).attr("title");
        var objLich = me.dtLichHoc.find(e => e.ID === me.strLichHoc_Id);
        //--Edit
        var obj_save = {
            'action': 'CC_NguoiHoc_ChuyenCan/ThemMoi',
            'strId': '',
            'strChucNang_Id': edu.system.strChucNang_Id,
            'strNguoiThucHien_Id': edu.system.userId,
            'strQLSV_NguoiHoc_Id': strSV_Id,
            'strDaoTao_LopQuanLy_Id': "",
            'strDaoTao_ChuongTrinh_Id': objSV.DAOTAO_TOCHUCCHUONGTRINH_ID,
            'strQLSV_TrangThaiNguoiHoc_Id': objSV.QLSV_TRANGTHAINGUOIHOC_ID,
            'strDiem_DanhSach_Id': objSV.DANGKY_LOPHOCPHAN_ID,
            'strKieuChuyenCan_Id': arrId[1],
            'strNgayGhiNhan': objLich.NGAYHOC,
            'dSoLuong': $("#" + point.id.replace("chkSelect", "input_")).val(),
            'dGio': objLich.GIOBATDAU,
            'dPhut': objLich.PHUTBATDAU,
            'dGiay': 0,
        };
        //default

        edu.system.makeRequest({
            success: function (data) {
                if (data.Success) {
                    edu.system.alert("Lưu thành công");
                }
                else {
                    edu.system.alert(obj_save.action + ": " + data.Message);
                }

                edu.system.start_Progress("divprogessdata", me.endSetData);
            },
            error: function (er) {
                edu.system.alert(obj_save.action + " (er): " + JSON.stringify(er), "w");

                edu.system.start_Progress("divprogessquanso", me.endGetData);
            },
            type: "POST",
            action: obj_save.action,

            contentType: true,

            data: obj_save,
            fakedb: [
            ]
        }, false, false, false, null);
    },
    delete_NhapChuyenCan: function (point) {
        var me = this;
        //--Edit
        var today = new Date();
        var obj = {};
        var strid = point.id;
        var arrId = strid.split("_");
        var strSV_Id = arrId[0].substring(9);
        var objSV = edu.util.objGetOneDataInData(strSV_Id, me.dtSinhVien, "ID");
        var objLich = me.dtLichHoc.find(e => e.ID === me.strLichHoc_Id);
        var obj_delete = {
            'action': 'CC_NguoiHoc_ChuyenCan/Xoa_QLSV_NguoiHoc_ChuyenCan',
            'strId': '',
            'strChucNang_Id': edu.system.strChucNang_Id,
            'strNguoiThucHien_Id': edu.system.userId,
            'strQLSV_NguoiHoc_Id': strSV_Id,
            'strDaoTao_LopQuanLy_Id': "",
            'strDaoTao_ChuongTrinh_Id': objSV.DAOTAO_TOCHUCCHUONGTRINH_ID,
            'strQLSV_TrangThaiNguoiHoc_Id': objSV.QLSV_TRANGTHAINGUOIHOC_ID,
            'strDiem_DanhSach_Id': objSV.DANGKY_LOPHOCPHAN_ID,
            'strKieuChuyenCan_Id': arrId[1],
            'strNgayGhiNhan': objLich.NGAYHOC,
            'dSoLuong': $("#" + point.id.replace("chkSelect", "input_")).val(),
            'strGio': objLich.GIOBATDAU,
            'strPhut': objLich.PHUTBATDAU,
            'strGiay': 0,
        };
        //default

        edu.system.makeRequest({
            success: function (data) {
                if (data.Success) {
                    edu.system.alert("Xóa thành công");
                }
                else {
                    edu.system.alert(data.Message);
                }

                edu.system.start_Progress("divprogessdata", me.endSetData);
            },
            error: function (er) {
                var obj = {
                    content: ": " + JSON.stringify(er),
                    code: "w"
                };
                edu.system.afterComfirm(obj);

                edu.system.start_Progress("divprogessquanso", me.endGetData);
            },
            type: 'POST',
            action: obj_delete.action,

            contentType: true,

            data: obj_delete,
            fakedb: [
            ]
        }, false, false, false, null);
    },


    save_TuGhiNhan: function (strId) {
        var me = this;
        var obj_notify = {};
        var today = new Date();
        console.log(edu.system.clientIP);
        var objLich = me.dtLichHoc.find(e => e.ID === me.strLichHoc_Id);
        var obj_save = {
            'action': 'CC_ThongTin/Them_QLSV_NguoiHoc_TuGhiNhan',
            'type': 'POST',
            'strChucNang_Id': edu.system.strChucNang_Id,
            'strDaoTao_LopQuanLy_Id': edu.util.getValById('dropAAAA'),
            'strDiem_DanhSach_Id': objLich.IDLOPHOCPHAN,
            'strNgayGhiNhan': objLich.NGAYHOC,
            'strGio': objLich.GIOBATDAU,
            'strPhut': objLich.PHUTBATDAU,
            'strGiay': 0,
            'strIp': edu.system.clientIP,
            'strNoiDungTuGhiNhan': edu.util.getValById('txtTuKhoaDiemDanh'),
            'strNguoiThucHien_Id': edu.system.userId,
            'strQLSV_NguoiHoc_Id': me.strGiangVien_Id,
            'strDaoTao_ChuongTrinh_Id': edu.util.getValById('dropAAAA'),
            'strQLSV_TrangThaiNguoiHoc_Id': edu.util.getValById('dropAAAA'),
        };
        //default
        edu.system.makeRequest({
            success: function (data) {
                if (data.Success) {
                    edu.system.alert("Thực hiện thành công!");
                }
                else {
                    obj_notify = {
                        type: "w",
                        content: obj_save.action + " (er): " + data.Message,
                    }
                    edu.system.alert(data.Message);
                }
            },
            error: function (er) {
                edu.system.alertOnModal(obj_notify);
            },
            type: "POST",
            action: obj_save.action,
            contentType: true,
            data: obj_save,
            fakedb: [
            ]
        }, false, false, false, null);
    },
    save_DiemDanhTuDong: function (strId) {
        var me = this;
        var obj_notify = {};
        var today = new Date();
        var obj_save = {
            'action': 'CC_GiangVien_TuGhiNhan/ThucHienDiemDanhTuDong',
            'type': 'POST',
            'strTuKhoa': edu.util.getValById('txtAAAA'),
            'strChucNang_Id': edu.system.strChucNang_Id,
            'strNgayGhiNhan': edu.util.dateToday(),
            'strGio': today.getHours(),
            'strPhut': today.getMinutes(),
            'strGiay': today.getSeconds(),
            'strDaoTao_LopHocPhan_Id': me.strLichHoc_Id,
            'strNguoiThucHien_Id': edu.system.userId,
        };
        //default
        edu.system.makeRequest({
            success: function (data) {
                if (data.Success) {
                    edu.system.alert("Thực hiện thành công!");
                }
                else {
                    obj_notify = {
                        type: "w",
                        content: obj_save.action + " (er): " + data.Message,
                    }
                    edu.system.alert(data.Message);
                }
            },
            error: function (er) {
                edu.system.alertOnModal(obj_notify);
            },
            type: "POST",
            action: obj_save.action,
            contentType: true,
            data: obj_save,
            fakedb: [
            ]
        }, false, false, false, null);
    },
    /*------------------------------------------
    --Discription: [3] AccessDB HOC
    --ULR:  Modules
    -------------------------------------------*/
    getList_LichThi: function (strNgayDangChon) {
        var me = this;
        //--Edit
        var obj_save = {
            'action': 'SV_ThongTin_MH/DSA4FRUNKCIpFSko',
            'func': 'pkg_congthongtin_hssv_thongtin.LayTTLichThi',
            'iM': edu.system.iM,
            'strQLSV_NguoiHoc_Id': me.strGiangVien_Id,
            'strNgayDangChon': strNgayDangChon,
        };
        //

        edu.system.makeRequest({
            success: function (data) {
                if (data.Success) {
                    var dtReRult = data.Data;
                    me.genTable_LichThi(dtReRult, data.Pager);
                }
                else {
                    edu.system.alert(data.Message, "s");
                }

            },
            error: function (er) {

                edu.system.alert(JSON.stringify(er), "w");
            },
            type: 'POST',
            action: obj_save.action,

            contentType: true,
            data: obj_save,
            fakedb: [

            ]
        }, false, false, false, null);
    },
    genTable_LichThi: function (data, iPager) {
        var me = this;
       
        var jsonForm = {
            strTable_Id: "tblLichThi",

            aaData: data,
            colPos: {
                center: [0],
            },
            aoColumns: [
                {
                    "mDataProp": "DANGKY_LOPHOCPHAN_TEN"
                },
                {
                    "mDataProp": "NGAYHOC",
                },
                {
                    "mData": "QLSV_NGUOIHOC_TEN",
                    mRender: function (nRow, objLich) {
                        return objLich.GIOBATDAU + "h" + objLich.PHUTBATDAU + " - " + objLich.GIOKETTHUC + "h" + objLich.PHUTKETTHUC
                    }
                },
                {
                    "mDataProp": "PHONGHOC_TEN",
                },
                {
                    "mDataProp": "TENHOCPHAN",
                }
            ]
        };

        edu.system.loadToTable_data(jsonForm);
    },
    
    getList_PhanChamThi: function () {
        var me = this;
        //--Edit
        var obj_save = {
            'action': 'XLHV_TP_PhanCong_MH/DSA4BRIVKSgVKSQuBS41FSko',
            'func': 'pkg_thi_phancong.LayDSThiTheoDotThi',
            'iM': edu.system.iM,
            'strTuKhoa': edu.util.getValById('txtSearch'),
            'strChucNang_Id': edu.system.strChucNang_Id,
            'dLocKhongHoanThanhNhapDiem': edu.util.getValById('dropSearch_HoanThanhNhapDiem'),
            'strThi_DotThi_Id': edu.util.getValById('dropSearch_DotThi'),
            'strDaoTao_HocPhan_Id': edu.util.getValById('dropSearch_MonThi'),
            'strTuNgay': edu.util.getValById('txtAAAA'),
            'strDenNgay': edu.util.getValById('txtAAAA'),
            'strNguoiThucHien_Id': edu.system.userId,
        };
        //
        edu.system.makeRequest({
            success: function (data) {
                if (data.Success) {
                    var dtReRult = data.Data;
                    me.dtPhanChamThi = dtReRult;
                    me.genTable_PhanChamThi(dtReRult, data.Pager);
                }
                else {
                    edu.system.alert(data.Message, "s");
                }
            },
            error: function (er) {

                edu.system.alert(JSON.stringify(er), "w");
            },
            type: "POST",
            action: obj_save.action,

            contentType: true,
            data: obj_save,
            fakedb: [

            ]
        }, false, false, false, null);
    },
    
    save_TangCamXuc: function (strLichHoc_Id, strEmo) {
        var me = this;
        var aData = me.dtLichHoc.find(e => e.ID == strLichHoc_Id);
        //--Edit
        var obj_save = {
            'action': 'SV_CamXuc_MH/FSAvJh4CICwZNCIP',
            'func': 'pkg_dg_camxuc_nguoihoc.Tang_CamXuc',
            'iM': edu.system.iM,
            'strChucNang_Id': edu.system.strChucNang_Id,
            'strDiem_DanhSachHoc_Id': aData.IDLOPHOCPHAN,
            'strNgayGhiNhan': aData.NGAYHOC,
            'dGio': aData.GIOBATDAU,
            'dPhut': aData.PHUTBATDAU,
            'dGiay': 0,
            'strDG_ChucNang_ChuDe_CT_Id': strEmo,
            'strDaoTao_ChuongTrinh_Id': edu.util.getValById('dropAAAA'),
            'strNguoiThucHien_Id': edu.system.userId,
        };
        //

        edu.system.makeRequest({
            success: function (data) {
                if (data.Success) {
                    me.getList_CamXucMacDinh(strLichHoc_Id, aData.IDLICHHOC)
                    //edu.system.alert("Thực hiện thành công");
                }
                else {
                    edu.system.alert(obj_save.action + " : " + data.Message, "s");
                }

            },
            error: function (er) {

                edu.system.alert(obj_save.action + " (er): " + JSON.stringify(er), "w");
            },
            type: "POST",
            action: obj_save.action,
            complete: function () {
                //edu.system.start_Progress("zoneprocessXXXX", function () {
                //    me.getList_PhanCong();
                //});
            },
            contentType: true,
            data: obj_save,
            fakedb: [

            ]
        }, false, false, false, null);
    },
    save_GiamCamXuc: function (strLichHoc_Id, strEmo) {
        var me = this;
        var aData = me.dtLichHoc.find(e => e.ID == strLichHoc_Id);
        //--Edit
        var obj_save = {
            'action': 'SV_CamXuc_MH/BiggLB4CICwZNCIP',
            'func': 'pkg_dg_camxuc_nguoihoc.Giam_CamXuc',
            'iM': edu.system.iM,
            'strChucNang_Id': edu.system.strChucNang_Id,
            'strDiem_DanhSachHoc_Id': aData.IDLOPHOCPHAN,
            'strNgayGhiNhan': aData.NGAYHOC,
            'dGio': aData.GIOBATDAU,
            'dPhut': aData.PHUTBATDAU,
            'dGiay': 0,
            'strDG_ChucNang_ChuDe_CT_Id': strEmo,
            'strDaoTao_ChuongTrinh_Id': edu.util.getValById('dropAAAA'),
            'strNguoiThucHien_Id': edu.system.userId,
        };
        //

        edu.system.makeRequest({
            success: function (data) {
                if (data.Success) {
                    me.getList_CamXucMacDinh(strLichHoc_Id, aData.IDLICHHOC)
                    //edu.system.alert("Thực hiện thành công");
                }
                else {
                    edu.system.alert(obj_save.action + " : " + data.Message, "s");
                }

            },
            error: function (er) {

                edu.system.alert(obj_save.action + " (er): " + JSON.stringify(er), "w");
            },
            type: "POST",
            action: obj_save.action,
            complete: function () {
                //edu.system.start_Progress("zoneprocessXXXX", function () {
                //    me.getList_PhanCong();
                //});
            },
            contentType: true,
            data: obj_save,
            fakedb: [

            ]
        }, false, false, false, null);
    },
    save_ThayDoiCamXuc: function (strLichHoc_Id, strEmo) {
        var me = this;
        console.log(strLichHoc_Id);
        var aData = me.dtLichHoc.find(e => e.ID == strLichHoc_Id);
        console.log(me.dtLichHoc);
        //--Edit
        var obj_save = {
            'action': 'SV_CamXuc_MH/FSkgOAUuKB4CICwZNCIP',
            'func': 'pkg_dg_camxuc_nguoihoc.ThayDoi_CamXuc',
            'iM': edu.system.iM,
            'strChucNang_Id': edu.system.strChucNang_Id,
            'strDiem_DanhSachHoc_Id': aData.IDLOPHOCPHAN,
            'strNgayGhiNhan': aData.NGAYHOC,
            'dGio': aData.GIOBATDAU,
            'dPhut': aData.PHUTBATDAU,
            'dGiay': 0,
            'strDG_ChucNang_ChuDe_CT_Id': strEmo,
            'strDaoTao_ChuongTrinh_Id': edu.util.getValById('dropAAAA'),
            'strNguoiThucHien_Id': edu.system.userId,
        };
        //

        edu.system.makeRequest({
            success: function (data) {
                if (data.Success) {
                    //edu.system.alert("Thực hiện thành công");
                    me.getList_CamXucMacDinh(strLichHoc_Id, aData.IDLICHHOC)
                }
                else {
                    edu.system.alert(obj_save.action + " : " + data.Message, "s");
                }

            },
            error: function (er) {

                edu.system.alert(obj_save.action + " (er): " + JSON.stringify(er), "w");
            },
            type: "POST",
            action: obj_save.action,
            complete: function () {
                //edu.system.start_Progress("zoneprocessXXXX", function () {
                //    me.getList_PhanCong();
                //});
            },
            contentType: true,
            data: obj_save,
            fakedb: [

            ]
        }, false, false, false, null);
    },


    getList_CamXucMacDinh: function (strId, strDiem_DanhSachHoc_Id) {
        var me = this;
        //--Edit
        var aData = me.dtLichHoc.find(e => e.ID == strId);

        var obj_save = {
            'action': 'SV_CamXuc_MH/DSA4FRUMICIFKC8p',
            'func': 'pkg_dg_camxuc_nguoihoc.LayTTMacDinh',
            'iM': edu.system.iM,
            'strChucNang_Id': edu.system.strChucNang_Id,
            'strDiem_DanhSachHoc_Id': aData.IDLOPHOCPHAN,
            'strNgayGhiNhan': aData.NGAYHOC,
            'dGio': aData.GIOBATDAU,
            'dPhut': aData.PHUTBATDAU,
            'dGiay': 0,
            'strNguoiThucHien_Id': edu.system.userId,
        };
        //
        edu.system.makeRequest({
            success: function (data) {
                if (data.Success) {
                    var dtReRult = data.Data;
                    if (dtReRult.length > 0) {
                        var aData = dtReRult[0];
                        var $oldEmo = $("#" + strId + " #mainicon");
                        var newImg = '<img id="mainicon" name="' + aData.ID + '" src="assets/images/eval-emoji/' + aData.DG_CHUCNANG_CHUDE_CHITIET_ANH + '" alt="" style="width:18px; height:18px; cursor:pointer; transition: transform 0.2s ease;" onmouseover="this.style.transform=\'scale(1.25)\'" onmouseout="this.style.transform=\'scale(1)\'">';
                        $oldEmo.replaceWith(newImg);
                        if (aData.SOLUONG) $("#lblSoLuong" + strId).html(edu.util.returnEmpty(aData.SOLUONG));
                        else $("#lblSoLuong" + strId).html("");
                    }
                }
                else {
                    edu.system.alert(data.Message, "s");
                }
            },
            error: function (er) {

                edu.system.alert(JSON.stringify(er), "w");
            },
            type: "POST",
            action: obj_save.action,

            contentType: true,
            data: obj_save,
            fakedb: [

            ]
        }, false, false, false, null);
    },

    getList_CamXuc: function (strId, strDiem_DanhSachHoc_Id) {
        var me = this;
        //--Edit

        var obj_save = {
            'action': 'SV_CamXuc_MH/DSA4BRICICwZNCIP',
            'func': 'pkg_dg_camxuc_nguoihoc.LayDSCamXuc',
            'iM': edu.system.iM,
            'strChucNang_Id': edu.system.strChucNang_Id,
            //'strDiem_DanhSachHoc_Id': strDiem_DanhSachHoc_Id,
            'strNgayGhiNhan': edu.util.getValById('txtAAAA'),
            'dGio': 0,
            'dPhut': 0,
            'dGiay': 0,
            'strNguoiThucHien_Id': edu.system.userId,
        };
        //
        edu.system.makeRequest({
            success: function (data) {
                if (data.Success) {
                    var dtReRult = data.Data;
                    var html = '';
                    dtReRult.forEach(aData => {
                        html += '<div class="item btnEmoChange" name="' + aData.ID +'">';
                        html += '<img src="assets/images/eval-emoji/' + aData.DG_CHUCNANG_CHUDE_CHITIET_ANH + '" alt="">';
                        html += '</div>';
                        //html += '<div class="item">';
                        //html += '<img src="assets/images/eval-emoji/1.png" alt="">';
                        //html += '<span>5</span>';
                        //html += '</div>';
                    })
                    me["strCamXuc"] = html;
                    //$("#listIcon" + strId).html(html);
                }
                else {
                    edu.system.alert(data.Message, "s");
                }
            },
            error: function (er) {

                edu.system.alert(JSON.stringify(er), "w");
            },
            type: "POST",
            action: obj_save.action,

            contentType: true,
            //async: false,
            data: obj_save,
            fakedb: [

            ]
        }, false, false, false, null);
    },
}