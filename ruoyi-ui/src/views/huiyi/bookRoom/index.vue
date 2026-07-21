<template>
  <div class="app-container">
    <!-- 顶部操作栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['huiyi:bookRoom:add']"
        >新增预约</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-refresh"
          size="mini"
          @click="refreshData"
        >刷新</el-button>
      </el-col>
      <el-col :span="6">
        <el-date-picker
          v-model="selectedDate"
          type="date"
          placeholder="选择日期"
          format="yyyy-MM-dd"
          value-format="yyyy-MM-dd"
          @change="handleDateChange"
          :picker-options="datePickerOptions"
          style="width: 100%"
        ></el-date-picker>
      </el-col>
      <el-col :span="6">
        <el-input
          v-model="searchKeyword"
          placeholder="输入会议室名称或位置进行搜索"
          clearable
          @keyup.enter.native="handleSearch"
          @clear="handleSearch"
        >
          <el-button slot="append" icon="el-icon-search" @click="handleSearch"></el-button>
        </el-input>
      </el-col>
    </el-row>

    <!-- 会议室状态说明 -->
    <el-alert
      :closable="false"
      style="margin-bottom: 20px;"
    >
      <div class="status-legend">
        <el-tag type="success" size="small">● 空闲</el-tag>
        <el-tag type="danger" size="small">● 占用</el-tag>
        <el-tag type="info" size="small">● 维修/关闭</el-tag>
      </div>
    </el-alert>

    <!-- 会议室卡片列表 -->
    <el-row :gutter="20">
      <el-col
        :xs="24"
        :sm="12"
        :md="8"
        :lg="6"
        :xl="4"
        v-for="room in filteredRooms"
        :key="room.id"
        style="margin-bottom: 20px;"
      >
        <el-card
          :class="getRoomStatusClass(room)"
          shadow="hover"
          @click.native="handleRoomClick(room)"
        >
          <div slot="header" class="room-header">
            <span class="room-name">{{ room.roomName }}</span>
            <el-tag
              :type="getRoomStatusTagType(room)"
              size="mini"
              class="room-status-tag"
            >
              {{ getRoomStatusText(room) }}
            </el-tag>
          </div>

          <!-- 会议室基本信息 -->
          <div class="room-info">
            <p><i class="el-icon-location"></i> {{ room.location }}</p>
            <p><i class="el-icon-user"></i> 容纳 {{ room.capacity }} 人</p>
            <p><i class="el-icon-notebook-2"></i> {{ room.description }}</p>
          </div>

          <!-- 时间轴显示预约情况 -->
          <div class="time-axis">
            <div class="time-label">00:00</div>
            <div class="time-line">
              <div
                v-for="booking in getRoomBookings(room.id)"
                :key="booking.bookingId"
                class="booking-slot"
                :style="getBookingStyle(booking)"
                @click.stop="handleBookingClick(booking)"
              >
                <div class="booking-tooltip">
                  <p><strong>{{ booking.bookPurpose }}</strong></p>
                  <p>{{ booking.startTime }} - {{ booking.endTime }}</p>
                  <p>预约人: {{ booking.empNo }}</p>
                </div>
              </div>
            </div>
            <div class="time-label">24:00</div>
          </div>

          <!-- 操作按钮 -->
          <div class="room-actions">
            <el-button
              size="mini"
              type="primary"
              @click.stop="handleBookRoom(room)"
              :disabled="room.status !== 'ACTIVE'"
            >
              {{ room.status === 'ACTIVE' ? '预约' : '不可用' }}
            </el-button>
            <el-button
              size="mini"
              :type="room.currentStatus === 'occupied' ? 'primary' : 'info'"
              @click.stop="viewRoomDetails(room)"
            >
              详情
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 无数据提示 -->
    <el-empty
      v-if="filteredRooms.length === 0"
      description="暂无符合条件的会议室"
    />

    <!-- 预约详情对话框 -->
    <el-dialog
      :title="detailDialogTitle"
      :visible.sync="detailDialogVisible"
      width="600px"
      append-to-body
    >
      <el-descriptions :column="1" border>
        <el-descriptions-item label="预约单号">{{ currentBooking.bookNo }}</el-descriptions-item>
        <el-descriptions-item label="会议室">{{ currentBooking.roomName }}</el-descriptions-item>
        <el-descriptions-item label="预约日期">{{ parseTime(currentBooking.bookDate, '{y}-{m}-{d}') }}</el-descriptions-item>
        <el-descriptions-item label="预约时间">{{ currentBooking.startTime }} - {{ currentBooking.endTime }}</el-descriptions-item>
        <el-descriptions-item label="预约事由">{{ currentBooking.bookPurpose }}</el-descriptions-item>
        <el-descriptions-item label="预约人">{{ currentBooking.empNo }}</el-descriptions-item>
        <el-descriptions-item label="预约状态">
          <el-tag
            v-if="currentBooking.bookStatus === '0'"
            type="info"
          >待确认</el-tag>
          <el-tag
            v-else-if="currentBooking.bookStatus === '1'"
            type="success"
          >已确认</el-tag>
          <el-tag
            v-else-if="currentBooking.bookStatus === '2'"
            type="danger"
          >已取消</el-tag>
          <el-tag
            v-else-if="currentBooking.bookStatus === '3'"
            type="warning"
          >已完成</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(currentBooking.createTime) }}</el-descriptions-item>
      </el-descriptions>

      <div slot="footer" class="dialog-footer">
        <el-button
          type="primary"
          @click="handleUpdate(currentBooking)"
          v-if="canModify(currentBooking)"
        >修改</el-button>
        <el-button
          @click="handleCancel(currentBooking)"
          v-if="canCancel(currentBooking)"
          :disabled="currentBooking.bookStatus === '2' || currentBooking.bookStatus === '3'"
        >取消</el-button>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </div>
    </el-dialog>

    <!-- 添加/修改预约对话框 -->
    <el-dialog
      :title="bookingDialogTitle"
      :visible.sync="bookingDialogVisible"
      width="600px"
      append-to-body
    >
      <el-form ref="bookingForm" :model="bookingForm" :rules="bookingRules" label-width="100px">
        <el-form-item label="会议室" prop="roomId">
          <el-input
            v-model="currentRoom.roomName"
            readonly
            placeholder="请选择会议室"
            style="width: 100%"
          >
            <span slot="suffix">{{ currentRoom.location }}</span>
          </el-input>
        </el-form-item>

        <el-form-item label="预约日期" prop="bookDate">
          <el-date-picker
            v-model="bookingForm.bookDate"
            type="date"
            placeholder="请选择预约日期"
            format="yyyy-MM-dd"
            value-format="yyyy-MM-dd"
            :picker-options="datePickerOptions"
            style="width: 100%"
            @change="handleBookingDateChange"
            :disabled="!!bookingForm.bookingId"
          ></el-date-picker>
        </el-form-item>

        <el-form-item label="开始时间" prop="startTime">
          <el-time-select
            v-model="bookingForm.startTime"
            :picker-options="{
              start: '00:00',
              step: '00:15',
              end: '24:00'
            }"
            placeholder="请选择开始时间"
            style="width: 49%;"
            @change="handleTimeChange"
            :disabled="!!bookingForm.bookingId"
          >
          </el-time-select>

          <span style="margin: 0 10px;">至</span>

          <el-time-select
            v-model="bookingForm.endTime"
            :picker-options="{
              start: '00:00',
              step: '00:15',
              end: '24:00',
              minTime: bookingForm.startTime
            }"
            placeholder="请选择结束时间"
            style="width: 49%;"
            :disabled="!!bookingForm.bookingId"
          >
          </el-time-select>
        </el-form-item>

        <el-form-item label="预约事由" prop="bookPurpose">
          <el-input
            v-model="bookingForm.bookPurpose"
            type="textarea"
            placeholder="请输入预约事由"
            :rows="3"
          />
        </el-form-item>

        <el-form-item v-if="showFreeTimeInfo && freeTimeRanges.length > 0" label="空闲时间">
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
        <el-button type="primary" @click="submitBookingForm">确 定</el-button>
        <el-button @click="cancelBooking">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listBookRoom,
  getBookRoom,
  addBookRoom,
  updateBookRoom,
  cancelBookRoom,
  getFreeTime,
  getCurrentStatusList
} from "@/api/huiyi/bookRoom";
import { listRoom } from "@/api/huiyi/room";

export default {
  name: "BookRoom",
  data() {
    return {
      // 会议室列表
      roomList: [],
      // 预约列表
      bookingList: [],
      // 日期选择
      selectedDate: this.getCurrentDate(),
      // 搜索关键词
      searchKeyword: '',
      // 过滤后的会议室列表
      filteredRooms: [],
      // 详情对话框
      detailDialogVisible: false,
      detailDialogTitle: '预约详情',
      currentBooking: {},
      // 预约对话框
      bookingDialogVisible: false,
      bookingDialogTitle: '',
      bookingForm: {},
      currentRoom: {},
      // 空闲时间信息
      freeTimeRanges: [],
      showFreeTimeInfo: false,
      // 日期选择器选项
      datePickerOptions: {
        disabledDate: (time) => {
          // 禁用过去日期
          return time.getTime() < Date.now() - 8.64e7;
        }
      },
      // 表单校验规则
      bookingRules: {
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
    this.loadData();
  },
  methods: {
    /** 获取当前日期 */
    getCurrentDate() {
      const now = new Date();
      const year = now.getFullYear();
      const month = String(now.getMonth() + 1).padStart(2, '0');
      const day = String(now.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    },

    /** 加载所有数据 */
    loadData() {
      Promise.all([
        this.loadRooms(),
        this.loadBookings()
      ]).then(() => {
        this.filterRooms();
      });
    },

    /** 加载会议室列表 */
    loadRooms() {
      return listRoom({}).then(response => {
        this.roomList = (response.rows || []).map(room => ({
          ...room,
          currentStatus:'unknown'
        }));
        this.filteredRooms = [...this.roomList];
      });
    },

    /** 加载预约列表 */
    loadBookings() {
      const query = {
        bookDate: this.selectedDate
      };
      return listBookRoom(query).then(response => {
        this.bookingList = response.rows || [];
      });
    },

    /** 过滤会议室 */
    filterRooms() {
      if (!this.searchKeyword) {
        this.filteredRooms = [...this.roomList];
      } else {
        const keyword = this.searchKeyword.toLowerCase();
        this.filteredRooms = this.roomList.filter(room => {
          return (
            room.roomName.toLowerCase().includes(keyword) ||
            room.location.toLowerCase().includes(keyword) ||
            room.description.toLowerCase().includes(keyword)
          );
        });
      }

      // 更新每个会议室的实时状态
      this.updateRoomStatus();
    },

    /** 更新会议室状态 */
    updateRoomStatus() {
      // 批量获取会议室状态
      const roomIds = this.filteredRooms.map(room => room.id);
      if (roomIds.length > 0) {
        console.log('发送的房间ID:', roomIds); // 调试信息
        getCurrentStatusList(roomIds).then(response => {
          console.log('接收到的状态数据:', response.data); // 调试信息

          // 创建房间ID到状态的映射，使用更严格的相等比较
          const statusMap = new Map();
          response.data.forEach(status => {
            statusMap.set(status.roomId, status); // 保持原始类型进行比较
          });

          console.log('构建的状态映射:', Object.fromEntries(statusMap)); // 调试信息

          // 更新会议室状态
          this.filteredRooms.forEach(room => {
            // 尝试多种匹配方式
            let statusInfo = null;

            // 方式1: 直接匹配（保持原始类型）
            if (statusMap.has(room.id)) {
              statusInfo = statusMap.get(room.id);
            }
            // 方式2: 数字-字符串转换匹配
            else if (typeof room.id === 'number' && statusMap.has(String(room.id))) {
              statusInfo = statusMap.get(String(room.id));
            }
            // 方式3: 字符串-数字转换匹配
            else if (typeof room.id === 'string' && statusMap.has(Number(room.id))) {
              statusInfo = statusMap.get(Number(room.id));
            }
            // 方式4: 遍历查找匹配
            else {
              for (let [key, value] of statusMap) {
                if (String(key) === String(room.id)) {
                  statusInfo = value;
                  break;
                }
              }
            }

            if (statusInfo) {
              console.log(`房间ID ${room.id} 匹配到状态: `, statusInfo);
              // 如果数据库中的状态不是ACTIVE，则保持为维护状态
              if (room.status !== 'ACTIVE') {
                room.currentStatus = 'maintaining';
              } else {
                // 否则根据实时占用情况设置
                room.currentStatus = statusInfo.occupied ? 'occupied' : 'available';
              }
            } else {
              console.log(`未找到房间ID为 ${room.id} 的状态信息，房间ID类型: ${typeof room.id}`);
              room.currentStatus = 'unknown';
            }
          });

          console.log('更新后的房间状态:', this.filteredRooms.map(r => ({
            id: r.id,
            status: r.currentStatus,
            occupied: r.currentStatus === 'occupied'
          }))); // 调试信息
        }).catch(error => {
          console.error('获取会议室状态失败:', error);
          // 出错时设置所有房间为unknown状态
          this.filteredRooms.forEach(room => {
            room.currentStatus = 'unknown';
          });
        });
      }
    },

    /** 获取会议室状态样式类 */
    getRoomStatusClass(room) {
      if (room.status !== 'ACTIVE') {
        return 'room-card maintaining';
      }
      return `room-card ${room.currentStatus || 'unknown'}`;
    },

    /** 获取会议室状态标签类型 */
    getRoomStatusTagType(room) {
      if (room.status !== 'ACTIVE') return 'info'; // 维修/关闭
      if (room.currentStatus === 'occupied') return 'danger';
      if (room.currentStatus === 'available') return 'success';
      return 'warning'; // 未知
    },

    /** 获取会议室状态文本 */
    getRoomStatusText(room) {
      if (room.status !== 'ACTIVE') return '维修/关闭';
      if (room.currentStatus === 'occupied') return '占用中';
      if (room.currentStatus === 'available') return '空闲';
      return '未知';
    },

    /** 获取指定会议室的预订记录 */
    getRoomBookings(roomId) {
      return this.bookingList.filter(booking =>
        booking.roomId == roomId &&
        booking.bookStatus !== '2' && // 不包括已取消的
        booking.bookStatus !== '3'   // 不包括已完成的
      );
    },

    /** 计算预订时间槽样式 */
    getBookingStyle(booking) {
      // 将时间转换为相对于00:00的位置百分比（24小时制）
      const startHour = parseInt(booking.startTime.split(':')[0]);
      const startMinute = parseInt(booking.startTime.split(':')[1]);
      const endHour = parseInt(booking.endTime.split(':')[0]);
      const endMinute = parseInt(booking.endTime.split(':')[1]);

      const startMinutes = startHour * 60 + startMinute;
      const endMinutes = endHour * 60 + endMinute;
      const totalSlotMinutes = 24 * 60; // 从0点到24点共24小时

      const top = (startMinutes / totalSlotMinutes) * 100;
      const height = ((endMinutes - startMinutes) / totalSlotMinutes) * 100;

      return {
        top: `${top}%`,
        height: `${height}%`,
        backgroundColor: booking.bookStatus === '0' ? '#E6F7FF' : '#FFECE6',
        border: booking.bookStatus === '0' ? '1px solid #1890FF' : '1px solid #FF4D4F'
      };
    },

    /** 日期改变事件 */
    handleDateChange(val) {
      if (val) {
        this.loadBookings().then(() => {
          this.filterRooms();
        });
      }
    },

    /** 搜索事件 */
    handleSearch() {
      this.filterRooms();
    },

    /** 刷新数据 */
    refreshData() {
      this.loadData();
    },

    /** 会议室卡片点击事件 */
    handleRoomClick(room) {
      // 显示该会议室的详细信息，可能包括日历视图等
      console.log('Clicked room:', room);
    },

    /** 预订会议室 */
    handleBookRoom(room) {
      this.resetBookingForm();
      this.currentRoom = { ...room };
      this.bookingForm.roomId = room.id;
      this.bookingForm.bookDate = this.selectedDate;
      this.bookingDialogTitle = "新增会议室预约";
      this.bookingDialogVisible = true;

      // 加载该会议室在所选日期的空闲时间
      this.loadFreeTimeInfo(room.id, this.selectedDate);
    },

    /** 查看会议室详情 */
    viewRoomDetails(room) {
      // 获取该会议室在当前选定日期的预约信息
      const roomBookings = this.getRoomBookings(room.id);
      if (roomBookings && roomBookings.length > 0) {
        // 如果有预约记录，则显示第一个预约的详情
        const firstBooking = roomBookings[0];
        getBookRoom(firstBooking.bookingId).then(response => {
          this.currentBooking = response.data;
          this.detailDialogTitle = "预约详情";
          this.detailDialogVisible = true;
        });
      } else {
        // 如果当天没有预约，显示提示信息
        this.$modal.msgInfo(`会议室 ${room.roomName} 在 ${this.selectedDate} 当天暂无预约`);
      }
    },

    /** 预订项点击事件 */
    handleBookingClick(booking) {
      getBookRoom(booking.bookingId).then(response => {
        this.currentBooking = response.data;
        this.detailDialogTitle = "预约详情";
        this.detailDialogVisible = true;
      });
    },

    /** 新增按钮操作 */
    handleAdd() {
      if (this.roomList.length === 0) {
        this.$modal.msgWarning("暂无可预订的会议室");
        return;
      }

      // 可以选择任意一个可用会议室
      const availableRoom = this.roomList.find(room => room.status === 'ACTIVE');
      if (!availableRoom) {
        this.$modal.msgWarning("暂无可预订的会议室");
        return;
      }

      this.handleBookRoom(availableRoom);
    },

    /** 修改按钮操作 */
    handleUpdate(row) {
      this.resetBookingForm();
      this.bookingForm = { ...row };

      // 查找对应的会议室信息
      const room = this.roomList.find(r => r.id == row.roomId);
      if (room) {
        this.currentRoom = { ...room };
      }

      this.bookingDialogTitle = "修改会议室预约";
      this.bookingDialogVisible = true;
      this.detailDialogVisible = false;
    },

    /** 取消预约操作 */
    handleCancel(row) {
      this.$modal.confirm('是否确认取消此会议室预约？').then(() => {
        return cancelBookRoom(row.bookingId, '');
      }).then(() => {
        this.detailDialogVisible = false;
        this.loadData(); // 重新加载数据
        this.$modal.msgSuccess("取消成功");
      }).catch(() => {});
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
    },

    /** 预订日期变更事件 */
    handleBookingDateChange(val) {
      if (this.bookingForm.roomId && val) {
        this.loadFreeTimeInfo(this.bookingForm.roomId, val);
      }
    },

    /** 时间变更事件 */
    handleTimeChange() {
      if (this.bookingForm.bookDate && this.bookingForm.roomId) {
        this.loadFreeTimeInfo(this.bookingForm.roomId, this.bookingForm.bookDate);
      }
    },

    /** 提交预订表单 */
    submitBookingForm() {
      this.$refs["bookingForm"].validate(valid => {
        if (valid) {
          if (this.bookingForm.bookingId != null) {
            updateBookRoom(this.bookingForm).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.bookingDialogVisible = false;
              this.loadData();
            });
          } else {
            addBookRoom(this.bookingForm).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.bookingDialogVisible = false;
              this.loadData();
            });
          }
        }
      });
    },

    /** 取消预订对话框 */
    cancelBooking() {
      this.bookingDialogVisible = false;
      this.resetBookingForm();
    },

    /** 重置预订表单 */
    resetBookingForm() {
      this.bookingForm = {
        bookingId: null,
        roomId: null,
        bookDate: this.selectedDate,
        startTime: null,
        endTime: null,
        bookPurpose: null
      };
      this.currentRoom = {};
      this.freeTimeRanges = [];
      this.showFreeTimeInfo = false;
      if (this.$refs["bookingForm"]) {
        this.$refs["bookingForm"].resetFields();
      }
    },

    /** 检查是否可修改 */
    canModify(booking) {
      return booking.bookStatus === '0' || booking.bookStatus === '1';
    },

    /** 检查是否可取消 */
    canCancel(booking) {
      return booking.bookStatus === '0' || booking.bookStatus === '1';
    }
  }
};
</script>

<style lang="scss" scoped>
.app-container {
  padding: 20px;
}

.mb8 {
  margin-bottom: 8px;
}

.status-legend {
  display: flex;
  gap: 15px;
  align-items: center;
}

.room-card {
  cursor: pointer;
  transition: all 0.3s ease;

  &.available {
    border-left: 4px solid #67C23A;
    &:hover {
      box-shadow: 0 2px 12px 0 rgba(103, 194, 58, 0.3);
    }
  }

  &.occupied {
    border-left: 4px solid #F56C6C;
    &:hover {
      box-shadow: 0 2px 12px 0 rgba(245, 108, 108, 0.3);
    }
  }

  &.maintaining {
    border-left: 4px solid #909399;
    &:hover {
      box-shadow: 0 2px 12px 0 rgba(144, 147, 153, 0.3);
    }
  }

  &.unknown {
    border-left: 4px solid #E6A23C;
    &:hover {
      box-shadow: 0 2px 12px 0 rgba(230, 162, 60, 0.3);
    }
  }
}

.room-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .room-name {
    font-weight: bold;
    font-size: 16px;
  }

  .room-status-tag {
    margin-left: 10px;
  }
}

.room-info {
  p {
    margin: 5px 0;
    font-size: 13px;
    color: #606266;
  }

  i {
    margin-right: 5px;
    color: #909399;
  }
}

.time-axis {
  position: relative;
  margin: 15px 0;
  height: 120px;
  display: flex;
  align-items: stretch;

  .time-label {
    position: absolute;
    font-size: 12px;
    color: #909399;
  }

  .time-label:first-child {
    top: -15px;
  }

  .time-label:last-child {
    bottom: -15px;
  }

  .time-line {
    flex: 1;
    position: relative;
    border-left: 1px dashed #dcdfe6;
    border-right: 1px dashed #dcdfe6;
    margin: 0 5px;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 1px;
      background: #dcdfe6;
    }

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 1px;
      background: #dcdfe6;
    }
  }

  .booking-slot {
    position: absolute;
    left: 5px;
    right: 5px;
    border-radius: 4px;
    cursor: pointer;
    padding: 2px;
    font-size: 12px;
    overflow: hidden;
    z-index: 10;

    &:hover {
      opacity: 0.8;

      .booking-tooltip {
        visibility: visible;
        opacity: 1;
      }
    }
  }

  .booking-tooltip {
    visibility: hidden;
    position: absolute;
    top: 100%;
    left: 0;
    background: white;
    border: 1px solid #ddd;
    border-radius: 4px;
    padding: 8px;
    z-index: 100;
    box-shadow: 0 2px 8px rgba(0,0,0,0.15);
    opacity: 0;
    transition: all 0.3s;
    min-width: 200px;

    p {
      margin: 3px 0;
      font-size: 12px;
    }

    strong {
      color: #303133;
    }
  }
}

.room-actions {
  text-align: right;
  padding-top: 10px;
  border-top: 1px solid #f4f4f5;
  margin-top: 10px;

  button {
    margin-left: 5px;
  }
}

.free-time-info {
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;

  .free-time-list {
    margin-top: 5px;
  }
}

::v-deep .el-card__header {
  padding: 12px 15px;
}

::v-deep .el-card__body {
  padding: 15px;
}
</style>
</template>