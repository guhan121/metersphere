<template>
  <el-dialog
    destroy-on-close
    :title="$t('load_test.runtime_config')"
    width="450px"
    :visible.sync="runModeVisible"
  >
    <div>
      <span class="ms-mode-span">{{ $t("run_mode.title") }}：</span>
      <el-radio-group v-model="runConfig.mode" @change="changeMode">
        <el-radio label="serial">{{ $t("run_mode.serial") }}</el-radio>
        <el-radio label="parallel">{{ $t("run_mode.parallel") }}</el-radio>
      </el-radio-group>
    </div>
<!--    <div class="ms-mode-div" v-if="runConfig.mode === 'serial'">-->
<!--      <span class="ms-mode-span">{{ $t("run_mode.other_config") }}：</span>-->
<!--      <el-radio-group v-model="runConfig.reportType">-->
<!--        <el-radio label="iddReport">{{ $t("run_mode.idd_report") }}</el-radio>-->
<!--        <el-radio label="setReport">{{ $t("run_mode.set_report") }}</el-radio>-->
<!--      </el-radio-group>-->
<!--    </div>-->
<!--    <div class="ms-mode-div" v-if="runConfig.reportType === 'setReport' && runConfig.mode==='serial'">-->
<!--      <span class="ms-mode-span">{{ $t("run_mode.report_name") }}：</span>-->
<!--      <el-input-->
<!--        v-model="runConfig.reportName"-->
<!--        :placeholder="$t('commons.input_content')"-->
<!--        size="small"-->
<!--        style="width: 200px"-->
<!--      />-->
<!--    </div>-->
    <template v-slot:footer>
      <ms-dialog-footer @cancel="close" @confirm="handleRunBatch"/>
    </template>
  </el-dialog>
</template>

<script>
import MsDialogFooter from "@/business/components/common/components/MsDialogFooter";

export default {
  name: "RunMode",
  components: {MsDialogFooter},
  data() {
    return {
      runModeVisible: false,
      testType: null,
      resourcePools: [],
      runConfig: {
        mode: "serial",
        reportType: "iddReport",
        reportName: "",
        runWithinResourcePool: false,
        resourcePoolId: null,
      },
    };
  },
  methods: {
    open() {
      this.runModeVisible = true;
      this.getResourcePools();
    },
    changeMode() {
      this.runConfig.runWithinResourcePool = false;
      this.runConfig.resourcePoolId = null;
      this.runConfig.reportType = "iddReport";
      this.runConfig.reportName = "";
    },
    close() {
      this.runConfig = {
        mode: "serial",
        reportType: "iddReport",
        reportName: "",
        runWithinResourcePool: false,
        resourcePoolId: null,
      };
      this.runModeVisible = false;
    },
    handleRunBatch() {
      if (this.runConfig.mode === 'serial' && this.runConfig.reportType === 'setReport' && this.runConfig.reportName.trim() === "") {
        this.$warning(this.$t('commons.input_name'));
        return;
      }
      this.$emit("handleRunBatch", this.runConfig);
      this.close();
    },
    getResourcePools() {
      this.result = this.$get('/testresourcepool/list/quota/valid', response => {
        this.resourcePools = response.data.filter(p => p.type === 'NODE');
      });
    },
  },
};
</script>

<style scoped>
.ms-mode-span {
  margin-right: 10px;
}

.ms-mode-div {
  margin-top: 20px;
}
</style>
