package io.metersphere.api.jmeter;

import io.metersphere.api.dto.automation.RunModeConfig;
import io.metersphere.api.dto.scenario.request.BodyFile;
import io.metersphere.base.domain.JarConfig;
import io.metersphere.commons.constants.ApiRunMode;
import io.metersphere.commons.constants.RunModeConstants;
import io.metersphere.commons.exception.MSException;
import io.metersphere.commons.utils.CommonBeanFactory;
import io.metersphere.commons.utils.CompressUtils;
import io.metersphere.commons.utils.LogUtil;
import io.metersphere.config.JmeterProperties;
import io.metersphere.i18n.Translator;
import io.metersphere.service.JarConfigService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.CSVDataSet;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.protocol.http.util.HTTPFileArg;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.backend.BackendListener;
import org.apache.jorphan.collections.HashTree;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Service
public class JMeterService {
    private static final String BASE_URL = "http://%s:%d";
    @Resource
    private JmeterProperties jmeterProperties;
    @Resource
    ResourcePoolCalculation resourcePoolCalculation;
    @Resource
    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        String JMETER_HOME = getJmeterHome();

        String JMETER_PROPERTIES = JMETER_HOME + "/bin/jmeter.properties";
        JMeterUtils.loadJMeterProperties(JMETER_PROPERTIES);
        JMeterUtils.setJMeterHome(JMETER_HOME);
        JMeterUtils.setLocale(LocaleContextHolder.getLocale());
    }

    public void run(String testId, String debugReportId, InputStream is) {
        try {
            Object scriptWrapper = SaveService.loadElement(is);
            HashTree testPlan = getHashTree(scriptWrapper);
            JMeterVars.addJSR223PostProcessor(testPlan);
            String runMode = StringUtils.isBlank(debugReportId) ? ApiRunMode.RUN.name() : ApiRunMode.DEBUG.name();
            this.runSerial(testId, testPlan, debugReportId, runMode, null);
        } catch (Exception e) {
            LogUtil.error(e.getMessage(), e);
            MSException.throwException(Translator.get("api_load_script_error"));
        }
    }

    public String getJmeterHome() {
        String home = getClass().getResource("/").getPath() + "jmeter";
        try {
            File file = new File(home);
            if (file.exists()) {
                return home;
            } else {
                return jmeterProperties.getHome();
            }
        } catch (Exception e) {
            return jmeterProperties.getHome();
        }
    }

    public static HashTree getHashTree(Object scriptWrapper) throws Exception {
        Field field = scriptWrapper.getClass().getDeclaredField("testPlan");
        field.setAccessible(true);
        return (HashTree) field.get(scriptWrapper);
    }

    public static void addBackendListener(String testId, String debugReportId, String runMode, HashTree testPlan, RunModeConfig config) {
        BackendListener backendListener = new BackendListener();
        backendListener.setName(testId);
        Arguments arguments = new Arguments();
        if (config != null && config.getMode().equals(RunModeConstants.SERIAL.toString()) && config.getReportType().equals(RunModeConstants.SET_REPORT.toString())) {
            arguments.addArgument(APIBackendListenerClient.TEST_REPORT_ID, config.getReportId());

        }
        arguments.addArgument(APIBackendListenerClient.TEST_ID, testId);
        if (StringUtils.isNotBlank(runMode)) {
            arguments.addArgument("runMode", runMode);
        }
        if (StringUtils.isNotBlank(debugReportId)) {
            arguments.addArgument("debugReportId", debugReportId);
        }
        backendListener.setArguments(arguments);
        backendListener.setClassname(APIBackendListenerClient.class.getCanonicalName());
        testPlan.add(testPlan.getArray()[0], backendListener);
    }

    /*
     * 立即启动测试线程测试；立即返回
     * 适用于页面点击调试
     */
    public void runDefinition(String testId, HashTree testPlan, String debugReportId, String runMode) {
        startRunner(testId, testPlan, debugReportId, runMode, null);
    }

    public void runSerial(String testId, HashTree testPlan, String debugReportId, String runMode, RunModeConfig config) {
        synchronized (JMeterService.class) {
            while (true) {
                int threadCounts = APIBackendListenerClient.getNumberOfThreads();
                LogUtil.info(" -> run:[" + testId + "];["+debugReportId+"]" + Thread.currentThread().getId() + ";current:" + threadCounts + ";act:" + JMeterContextService.getNumberOfThreads());

                if (threadCounts >= 50) {
                    //如果当前测试线程大于50个则需要等待
                    try {
                        Thread.sleep(5000);
                        LogUtil.info(String.format(" -> 当前线程数限制;休眠五秒 [%s]", testId));
                    } catch (Exception ignored) {
                    }
                } else {
                    break;
                }
            }
            startRunner(testId, testPlan, debugReportId, runMode, config);
        }
    }

    private void startRunner(String testId, HashTree testPlan, String debugReportId, String runMode, RunModeConfig config) {
        try {
            init();
            APIBackendListenerClient.incrNumberOfThreads();
            addBackendListener(testId, debugReportId, runMode, testPlan, config);
            LocalRunner runner = new LocalRunner(testPlan);
            //SaveService.saveTree(testPlan, new FileOutputStream("/opt/metersphere/logs/metersphere/" + testId.replace("\"", "") + ".jmx"));
            runner.run();
        } catch (Exception e) {
            APIBackendListenerClient.decrNumberOfThreads();
            LogUtil.error(e.getMessage(), e);
            MSException.throwException(Translator.get("api_load_script_error"));
        }
    }

    /**
     * 获取当前jmx 涉及到的文件
     *
     * @param tree
     */
    public void getFiles(HashTree tree, List<BodyFile> files) {
        for (Object key : tree.keySet()) {
            HashTree node = tree.get(key);
            if (key instanceof HTTPSamplerProxy) {
                HTTPSamplerProxy source = (HTTPSamplerProxy) key;
                if (source != null && source.getHTTPFiles().length > 0) {
                    for (HTTPFileArg arg : source.getHTTPFiles()) {
                        BodyFile file = new BodyFile();
                        file.setId(arg.getParamName());
                        file.setName(arg.getPath());
                        files.add(file);
                    }
                }
            } else if (key instanceof CSVDataSet) {
                CSVDataSet source = (CSVDataSet) key;
                if (source != null && source.getFilename() != null) {
                    BodyFile file = new BodyFile();
                    file.setId(source.getFilename());
                    file.setName(source.getFilename());
                    files.add(file);
                }
            }
            if (node != null) {
                getFiles(node, files);
            }
        }
    }

    private byte[] fileToByte(File tradeFile) {
        byte[] buffer = null;
        try (FileInputStream fis = new FileInputStream(tradeFile);
             ByteArrayOutputStream bos = new ByteArrayOutputStream();) {
            byte[] b = new byte[1024];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            buffer = bos.toByteArray();
        } catch (Exception e) {
        }
        return buffer;
    }

    private List<Object> getZipJar() {
        List<Object> jarFiles = new LinkedList<>();
        // jar 包
        JarConfigService jarConfigService = CommonBeanFactory.getBean(JarConfigService.class);
        List<JarConfig> jars = jarConfigService.list();
        List<File> files = new ArrayList<>();

        jars.forEach(jarConfig -> {
            String path = jarConfig.getPath();
            File file = new File(path);
            if (file.isDirectory() && !path.endsWith("/")) {
                file = new File(path + "/");
            }
            files.add(file);
        });

        try {
            File file = CompressUtils.zipFiles(UUID.randomUUID().toString() + ".zip", files);
            FileSystemResource resource = new FileSystemResource(file);
            byte[] fileByte = this.fileToByte(file);
            if (fileByte != null) {
                ByteArrayResource byteArrayResource = new ByteArrayResource(fileByte) {
                    @Override
                    public String getFilename() throws IllegalStateException {
                        return resource.getFilename();
                    }
                };
                jarFiles.add(byteArrayResource);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return jarFiles;
    }

    private List<Object> getJar() {
        List<Object> jarFiles = new LinkedList<>();
        // jar 包
        JarConfigService jarConfigService = CommonBeanFactory.getBean(JarConfigService.class);
        List<JarConfig> jars = jarConfigService.list();
        jars.forEach(jarConfig -> {
            try {
                String path = jarConfig.getPath();
                File file = new File(path);
                if (file.isDirectory() && !path.endsWith("/")) {
                    file = new File(path + "/");
                }
                FileSystemResource resource = new FileSystemResource(file);
                byte[] fileByte = this.fileToByte(file);
                if (fileByte != null) {
                    ByteArrayResource byteArrayResource = new ByteArrayResource(fileByte) {
                        @Override
                        public String getFilename() throws IllegalStateException {
                            return resource.getFilename();
                        }
                    };
                    jarFiles.add(byteArrayResource);
                }

            } catch (Exception e) {
                LogUtil.error(e.getMessage(), e);
            }
        });
        return jarFiles;
    }

    private List<Object> getMultipartFiles(HashTree hashTree) {
        List<Object> multipartFiles = new LinkedList<>();
        // 获取附件
        List<BodyFile> files = new LinkedList<>();
        getFiles(hashTree, files);
        if (CollectionUtils.isNotEmpty(files)) {
            for (BodyFile bodyFile : files) {
                File file = new File(bodyFile.getName());
                if (file != null && !file.exists()) {
                    FileSystemResource resource = new FileSystemResource(file);
                    byte[] fileByte = this.fileToByte(file);
                    if (fileByte != null) {
                        ByteArrayResource byteArrayResource = new ByteArrayResource(fileByte) {
                            @Override
                            public String getFilename() throws IllegalStateException {
                                return resource.getFilename();
                            }
                        };
                        multipartFiles.add(byteArrayResource);
                    }
                }
            }
        }
        return multipartFiles;
    }

}
