package com.ruoyi.huiyi.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.huiyi.domain.MeetingFolder;
import com.ruoyi.huiyi.service.IMeetingFolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/huiyi/folder")
public class MeetingFolderController extends BaseController {

    @Autowired
    private IMeetingFolderService meetingFolderService;

    @PreAuthorize("@ss.hasPermi('huiyi:folder:list')")
    @GetMapping("/list")
    public TableDataInfo list(MeetingFolder meetingFolder)
    {
        startPage();
        List<MeetingFolder> list = meetingFolderService.selectMeetingFolderList(meetingFolder);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('huiyi:folder:query')")
    @GetMapping(value = "/{folderId}")
    public AjaxResult getInfo(@PathVariable Long folderId)
    {
        return success(meetingFolderService.selectMeetingFolderById(folderId));
    }

    @PreAuthorize("@ss.hasPermi('huiyi:folder:add')")
    @Log(title = "会议文件夹", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MeetingFolder meetingFolder)
    {
        return toAjax(meetingFolderService.insertMeetingFolder(meetingFolder));
    }

    @PreAuthorize("@ss.hasPermi('huiyi:folder:edit')")
    @Log(title = "会议文件夹", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MeetingFolder meetingFolder)
    {
        return toAjax(meetingFolderService.updateMeetingFolder(meetingFolder));
    }

    @PreAuthorize("@ss.hasPermi('huiyi:folder:remove')")
    @Log(title = "会议文件夹", businessType = BusinessType.DELETE)
    @DeleteMapping("/{folderIds}")
    public AjaxResult remove(@PathVariable Long[] folderIds)
    {
        return toAjax(meetingFolderService.deleteMeetingFolderByIds(folderIds));
    }
}
