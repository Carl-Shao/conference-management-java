<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="员工工号" prop="empNo">
        <el-input
          v-model="queryParams.empNo"
          placeholder="请输入员工工号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="会议室ID" prop="roomId">
        <el-input
          v-model="queryParams.roomId"
          placeholder="请输入会议室ID"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="预约日期" prop="bookDate">
        <el-date-picker
          v-model="queryParams.bookDate"
          type="date"
          placeholder="请选择预约日期"
          format="yyyy-MM-dd"
          value-format="yyyy-MM-dd"
        ></el-date-picker>
      </el-form-item>
      <el-form-item label="预约状态" prop="bookStatus">
        <el-select v-model="queryParams.bookStatus" placeholder="请选择预约状态" clearable>
          <el-option label="待确认" value="0" />
          <el-option label="已确认" value="1" />
          <el-option label="已取消" value="2" />
          <el-option label="已完成" value="3" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['huiyi:bookRoom:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['huiyi:bookRoom:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-close"
          size="mini"
          :disabled="multiple"
          @click="handleCancelSelected"
          v-hasPermi="['huiyi:bookRoom:cancel']"
        >批量取消</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="bookRoomList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="预约ID" align="center" prop="bookingId" />
      <el-table-column label="预约单号" align="center" prop="bookNo" />
      <el-table-column label="员工工号" align="center" prop="empNo" />
      <el-table-column label="会议室名称" align="center" prop="roomName" />
      <el-table-column label="预约日期" align="center" prop="bookDate" width="120">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.bookDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" align="center" prop="startTime" />
      <el-table-column label="结束时间" align="center" prop="endTime" />
      <el-table-column label="预约事由" align="center" prop="bookPurpose" show-tooltip-when-overflow />
      <el-table-column label="预约状态" align="center" prop="bookStatus">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.bookStatus === '0'" type="info">待确认</el-tag>
          <el-tag v-else-if="scope.row.bookStatus === '1'" type="success">已确认</el-tag>
          <el-tag v-else-if="scope.row.bookStatus === '2'" type="danger">已取消</el-tag>
          <el-tag v-else-if="scope.row.bookStatus === '3'" type="warning">已完成</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['huiyi:bookRoom:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-close"
            @click="handleCancel(scope.row)"
            v-if="scope.row.bookStatus !== '2'"
            v-hasPermi="['huiyi:bookRoom:cancel']"
          >取消</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改会议室预约对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="会议室" prop="roomId">
          <el-select v-model="form.roomId" placeholder="请选择会议室" style="width: 100%">
            <el-option
              v-for="item in roomOptions"
              :key="item.id"
              :label="item.roomName"
              :value="item.id"
              :disabled="item.disabled"
            >
              <span>{{ item.roomName }}</span>
              <span style="float: right; color: #8492a6; font-size: 13px">{{ item.location }}</span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="预约日期" prop="bookDate">
          <el-date-picker
            v-model="form.bookDate"
            type="date"
            placeholder="请选择预约日期"
            format="yyyy-MM-dd"
            value-format="yyyy-MM-dd"
            :picker-options="datePickerOptions"
            style="width: 100%"
            @change="handleDateChange"
          ></el-date-picker>
        </el-form-item>

        <el-form-item label="开始时间" prop="startTime">
          <el-time-select
            v-model="form.startTime"
            :picker-options="{
              start: '08:00',
              step: '00:15',
              end: '22:00'
            }"
            placeholder="请选择开始时间"
            style="width: 49%;"
            @change="handleTimeChange"
          >
          </el-time-select>

          <span style="margin: 0 10px;">至</span>

          <el-time-select
            v-model="form.endTime"
            :picker-options="{
              start: '08:00',
              step: '00:15',
              end: '22:00',
              minTime: form.startTime
            }"
            placeholder="请选择结束时间"
            style="width: 49%;"
          >
          </el-time-select>
        </el-form-item>

        <el-form-item label="预约事由" prop="bookPurpose">
          <el-input v-model="form.bookPurpose" type="textarea" placeholder="请输入预约事由" />
        </el-form-item>

        <el-form-item v-if="showFreeTimeInfo" label="空闲时间">
          <div class="free-time-info">
            <p>该日期该会议室的空闲时间：</p>
            <div class="free-time-list">
              <el-tag
                v-for="(range, index) in freeTimeRanges"
                :key="index"
                type="success"
                size="small"
                style="margin-right: 5px; margin-bottom: 5px;"
              >
                {{ range.startTime }} - {{ range.endTime }}
              </el-tag>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 预订状态批量查看 -->
    <el-dialog title="会议室占用状态" :visible.sync="statusDialogVisible" width="80%" append-to-body>
      <div class="status-grid">
        <div v-for="status in currentStatusList" :key="status.roomId" class="status-item">
          <div class="status-circle" :class="{ occupied: status.occupied, free: !status.occupied }"></div>
          <div class="status-room-name">{{ status.roomName }}</div>
          <div class="status-info">
            <span v-if="status.occupied">
              <span style="color: red;">占用中</span><br/>
              <small>事由: {{ status.currentPurpose || '无' }}</small><br/>
              <small>结束时间: {{ status.currentEndTime || '未知' }}</small>
            </span>
            <span v-else style="color: green;">空闲</span>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listBookRoom, getBookRoom, delBookRoom, addBookRoom, updateBookRoom, cancelBookRoom, getFreeTime, getCurrentStatusList } from "@/api/huiyi/bookRoom";
import { listRoom } from "@/api/huiyi/room";

export default {
  name: "BookRoom",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 会议室预约表格数据
      bookRoomList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 是否显示状态对话框
      statusDialogVisible: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        empNo: null,
        roomId: null,
        bookDate: null,
        bookStatus: null,
      },
      // 表单参数
      form: {},
      // 房间选项
      roomOptions: [],
      // 空闲时间范围
      freeTimeRanges: [],
      // 是否显示空闲时间信息
      showFreeTimeInfo: false,
      // 当前状态列表
      currentStatusList: [],
      // 日期选择器选项
      datePickerOptions: {
        disabledDate: (time) => {
          // 禁用过去日期
          return time.getTime() < Date.now() - 8.64e7;
        }
      },
      // 表单校验
      rules: {
        roomId: [
          { required: true, message: "会议室不能为空", trigger: "change" }
        ],
        bookDate: [
          { required: true, message: "预约日期不能为空", trigger: "change" }
        ],
        startTime: [
          { required: true, message: "开始时间不能为空", trigger: "change" }
        ],
        endTime: [
          { required: true, message: "结束时间不能为空", trigger: "change" }
        ],
        bookPurpose: [
          { required: true, message: "预约事由不能为空", trigger: "blur" }
        ]
      }
    };
  },
  created() {
    this.getList();
    this.loadRoomOptions();
  },
  methods: {
    /** 查询会议室预约列表 */
    getList() {
      this.loading = true;
      listBookRoom(this.queryParams).then(response => {
        this.bookRoomList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    /** 查询会议室列表 */
    loadRoomOptions() {
      listRoom({}).then(response => {
        this.roomOptions = response.rows.map(room => ({
          id: room.id,
          roomName: room.roomName,
          location: room.location,
          disabled: room.status !== null && room.status !== '0' // 如果状态不是0（正常），则禁用
        }));
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        bookingId: null,
        roomId: null,
        bookDate: null,
        startTime: null,
        endTime: null,
        bookPurpose: null,
        meetingId: null
      };
      this.freeTimeRanges = [];
      this.showFreeTimeInfo = false;
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.bookingId);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加会议室预约";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const bookingId = row.bookingId || this.ids;

      // 只有已确认和待确认的预约可以修改
      if (row.bookStatus === '2' || row.bookStatus === '3') {
        this.$modal.msgError("只有待确认和已确认的预约才能修改");
        return;
      }

      getBookRoom(bookingId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改会议室预约";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.bookingId != null) {
            updateBookRoom(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addBookRoom(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作（取消预约） */
    handleDelete(row) {
      const bookingIds = row.bookingId || this.ids;
      this.$modal.confirm('是否确认取消选中的会议室预约？').then(function() {
        return cancelBookRoom(bookingIds, '');
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("取消成功");
      }).catch(() => {});
    },
    /** 批量取消预约操作 */
    handleCancelSelected() {
      if (this.ids.length === 0) {
        this.$modal.msgError("请先选择要取消的预约");
        return;
      }

      this.$modal.confirm('是否确认取消选中的' + this.ids.length + '个会议室预约？').then(() => {
        return cancelBookRoom(this.ids.join(','), '');
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("取消成功");
      }).catch(() => {});
    },
    /** 取消预约操作 */
    handleCancel(row) {
      this.$modal.confirm('是否确认取消此会议室预约？').then(() => {
        return cancelBookRoom(row.bookingId, '');
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("取消成功");
      }).catch(() => {});
    },
    /** 日期变更事件 */
    handleDateChange(val) {
      if (this.form.roomId && val) {
        this.loadFreeTimeInfo(this.form.roomId, val);
      }
    },
    /** 时间变更事件 */
    handleTimeChange(val) {
      if (this.form.bookDate && this.form.roomId) {
        this.loadFreeTimeInfo(this.form.roomId, this.form.bookDate);
      }
    },
    /** 加载空闲时间信息 */
    loadFreeTimeInfo(roomId, bookDate) {
      getFreeTime(roomId, bookDate).then(response => {
        const data = response.data;
        if (data && data.freeRanges) {
          this.freeTimeRanges = data.freeRanges;
          this.showFreeTimeInfo = true;
        } else {
          this.freeTimeRanges = [];
          this.showFreeTimeInfo = false;
        }
      }).catch(error => {
        console.error('获取空闲时间失败:', error);
        this.freeTimeRanges = [];
        this.showFreeTimeInfo = false;
      });
    }
  }
};
</script>

<style scoped>
.status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 15px;
}

.status-item {
  border: 1px solid #e6e6e6;
  padding: 10px;
  border-radius: 4px;
  text-align: center;
}

.status-circle {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: inline-block;
  margin-right: 5px;
}

.status-circle.occupied {
  background-color: red;
}

.status-circle.free {
  background-color: green;
}

.status-room-name {
  font-weight: bold;
  margin: 5px 0;
}

.status-info {
  font-size: 12px;
}

.free-time-info {
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.free-time-list {
  margin-top: 5px;
}
</style>
</template>
</script>