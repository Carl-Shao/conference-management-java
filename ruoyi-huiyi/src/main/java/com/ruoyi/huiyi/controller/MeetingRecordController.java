package com.ruoyi.huiyi.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.huiyi.domain.MeetingRecord;
import com.ruoyi.huiyi.domain.dto.MeetingMergeDTO;
import com.ruoyi.huiyi.domain.dto.MeetingMoveFolderDTO;
import com.ruoyi.huiyi.domain.vo.MeetingCreateVO;
import com.ruoyi.huiyi.domain.vo.MeetingDetailVO;
import com.ruoyi.huiyi.service.IMeetingRecordService;
import com.ruoyi.huiyi.util.HttpRangeUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.List;

/**
 * 会议纪要
 *
 * "全部会议纪要" 与 "只看收藏" 两个前端页面共用这一个 controller：
 *  - 全部会议纪要页面：GET /huiyi/record/list                （不传 isFavorite）
 *  - 只看收藏页面：    GET /huiyi/record/list?isFavorite=1
 *  两者都支持 sourceType 筛选、title/createTime 排序（前端用 params.orderByColumn 传）
 */
@RestController
@RequestMapping("/huiyi/record")
public class MeetingRecordController extends BaseController {

    @Autowired
    private IMeetingRecordService meetingRecordService;

    /** 会议记录列表（全部 / 收藏 / 文件夹内，三合一） */
    @PreAuthorize("@ss.hasPermi('huiyi:record:list')")
    @GetMapping("/list")
    public TableDataInfo list(MeetingRecord meetingRecord) {
        meetingRecord.setCreateBy(SecurityUtils.getUsername());
        startPage();
        List<MeetingRecord> list = meetingRecordService.selectMeetingRecordList(meetingRecord);
        return getDataTable(list);
    }

    /** 点击某条记录后跳转到详情页，一次性拿转写+纪要+笔记 */
    @PreAuthorize("@ss.hasPermi('huiyi:record:query')")
    @GetMapping(value = "/{meetingId}")
    public AjaxResult getDetail(@PathVariable("meetingId") Long meetingId) {
        MeetingDetailVO vo = meetingRecordService.selectMeetingDetail(meetingId);
        return success(vo);
    }

    @Log(title = "会议纪要", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MeetingRecord meetingRecord) {
        meetingRecordService.insertMeetingRecord(meetingRecord);
        MeetingCreateVO vo = new MeetingCreateVO();
        vo.setMeetingId(meetingRecord.getMeetingId());
        vo.setMeetingTitle(meetingRecord.getTitle());
        return success(vo);
    }

    @PreAuthorize("@ss.hasPermi('huiyi:record:edit')")
    @Log(title = "会议纪要", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MeetingRecord meetingRecord) {
        return toAjax(meetingRecordService.updateMeetingRecord(meetingRecord));
    }

    @PreAuthorize("@ss.hasPermi('huiyi:record:remove')")
    @Log(title = "会议纪要", businessType = BusinessType.DELETE)
    @DeleteMapping("/{meetingIds}")
    public AjaxResult remove(@PathVariable Long[] meetingIds) {
        return toAjax(meetingRecordService.deleteMeetingRecordByIds(meetingIds));
    }

    /** 重命名 */
    @PreAuthorize("@ss.hasPermi('huiyi:record:edit')")
    @Log(title = "会议纪要重命名", businessType = BusinessType.UPDATE)
    @PutMapping("/{meetingId}/rename")
    public AjaxResult rename(@PathVariable Long meetingId, @RequestParam String title) {
        return toAjax(meetingRecordService.renameMeeting(meetingId, title));
    }

    /** 收藏 / 取消收藏 */
    @PreAuthorize("@ss.hasPermi('huiyi:record:edit')")
    @PutMapping("/{meetingId}/favorite")
    public AjaxResult favorite(@PathVariable Long meetingId, @RequestParam boolean favorite) {
        return toAjax(meetingRecordService.toggleFavorite(meetingId, favorite));
    }

    /** 批量移动到文件夹（folderId 传空表示移出文件夹） */
    @PreAuthorize("@ss.hasPermi('huiyi:record:edit')")
    @Log(title = "会议纪要移动文件夹", businessType = BusinessType.UPDATE)
    @PutMapping("/moveFolder")
    public AjaxResult moveFolder(@RequestBody MeetingMoveFolderDTO dto) {
        return toAjax(meetingRecordService.moveToFolder(dto));
    }

    /** 合并多条会议记录 */
    @PreAuthorize("@ss.hasPermi('huiyi:record:edit')")
    @Log(title = "会议纪要合并", businessType = BusinessType.UPDATE)
    @PostMapping("/merge")
    public AjaxResult merge(@RequestBody MeetingMergeDTO dto) {
        Long newMeetingId = meetingRecordService.mergeMeetings(dto);
        return success(newMeetingId);
    }

    /** 保存/更新用户笔记 */
    @PreAuthorize("@ss.hasPermi('huiyi:record:edit')")
    @PutMapping("/{meetingId}/note")
    public AjaxResult saveNote(@PathVariable Long meetingId, @RequestBody String content) {
        return toAjax(meetingRecordService.saveNote(meetingId, content));
    }

    /** 文件流接口 */
    @PreAuthorize("@ss.hasPermi('huiyi:record:query')")
    @GetMapping("/{meetingId}/audio")
    public void streamAudio(@PathVariable Long meetingId, HttpServletRequest request,
                            HttpServletResponse response) throws IOException {
        File file;
        try{
            file = meetingRecordService.resolvePlayableAudioFile(meetingId);
        } catch(ServiceException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if(file == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        long fileLength = file.length();
        HttpRangeUtils.RangeResult range = HttpRangeUtils.parse(request.getHeader("Range"), fileLength);

        response.setContentType("audio/wav");
        response.setHeader("Accept-Ranges", "bytes");

        if (range.isRangeRequest && !range.satisfiable)
        {
            response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            response.setHeader("Content-Range", "bytes */" + fileLength);
            return;
        }
        if (range.isRangeRequest)
        {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader("Content-Range", "bytes " + range.start + "-" + range.end + "/" + fileLength);
        }
        else
        {
            response.setStatus(HttpServletResponse.SC_OK);
        }
        response.setContentLengthLong(range.end - range.start + 1);

        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             OutputStream os = response.getOutputStream()) {
            raf.seek(range.start);
            byte[] buffer = new byte[8192];
            long remaining = range.end - range.start + 1;
            int read;
            while(remaining > 0 && (read = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                os.write(buffer, 0, read);
                remaining -= read;
            }
            os.flush();
        } catch (IOException e){}
    }
}
