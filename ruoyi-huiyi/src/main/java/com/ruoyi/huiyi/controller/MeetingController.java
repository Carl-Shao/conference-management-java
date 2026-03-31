package com.ruoyi.huiyi.controller;

import java.util.Arrays;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.huiyi.domain.Meeting;
import com.ruoyi.huiyi.service.IMeetingService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 会议信息 Controller
 * 
 * @author ruoyi
 * @date 2026-03-26
 */
@RestController
@RequestMapping("/huiyi/meeting")
public class MeetingController extends BaseController
{
    @Autowired
    private IMeetingService meetingService;

    /**
     * 查询会议信息列表
     */
    @PreAuthorize("@ss.hasPermi('huiyi:meeting:list')")
    @GetMapping("/list")
    public TableDataInfo list(Meeting meeting)
    {
        startPage();
        List<Meeting> list = meetingService.selectMeetingList(meeting);
        return getDataTable(list);
    }

    /**
     * 导出会议信息列表
     */
    @PreAuthorize("@ss.hasPermi('huiyi:meeting:export')")
    @Log(title = "会议信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Meeting meeting)
    {
        List<Meeting> list = meetingService.selectMeetingList(meeting);
        ExcelUtil<Meeting> util = new ExcelUtil<Meeting>(Meeting.class);
        util.exportExcel(response, list, "会议信息数据");
    }

    /**
     * 获取会议信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('huiyi:meeting:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(meetingService.selectMeetingById(id));
    }

    /**
     * 新增会议信息
     */
    @PreAuthorize("@ss.hasPermi('huiyi:meeting:add')")
    @Log(title = "会议信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Meeting meeting)
    {
        return toAjax(meetingService.insertMeeting(meeting));
    }

    /**
     * 修改会议信息
     */
    @PreAuthorize("@ss.hasPermi('huiyi:meeting:edit')")
    @Log(title = "会议信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Meeting meeting)
    {
        return toAjax(meetingService.updateMeeting(meeting));
    }

    /**
     * 删除会议信息
     */
    @PreAuthorize("@ss.hasPermi('huiyi:meeting:remove')")
    @Log(title = "会议信息", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(meetingService.deleteMeetingByIds(ids));
    }
}
