package org.openthinclient.api.rest.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BootOptions {

    @JsonIgnore
    private final Map<String, Object> additionalProperties = new HashMap<String, Object>();
    @JsonProperty(value = "NFSRootserver", required = true)
    @NotNull
    private String nfsRootserver;
    @JsonProperty(value = "NFSRootPath", required = true)
    @NotNull
    private String nfsRootPath;
    @JsonProperty(value = "TFTPBootserver", required = true)
    @NotNull
    private String tftpBootserver;
    @JsonProperty(value = "BootfileName", required = true)
    @NotNull
    private String bootfileName;
    @JsonProperty(value = "KernelName", required = true)
    @NotNull
    private String kernelName;
    @JsonProperty(value = "InitrdName", required = true)
    @NotNull
    private String initrdName;

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public String getNFSRootserver() {
        return nfsRootserver;
    }

    public void setNFSRootserver(String NFSRootserver) {
        this.nfsRootserver = NFSRootserver;
    }

    public String getNFSRootPath() {
        return nfsRootPath;
    }

    public void setNFSRootPath(String NFSRootPath) {
        this.nfsRootPath = NFSRootPath;
    }

    public String getTFTPBootserver() {
        return tftpBootserver;
    }

    public void setTFTPBootserver(String TFTPBootserver) {
        this.tftpBootserver = TFTPBootserver;
    }

    public String getBootfileName() {
        return bootfileName;
    }

    public void setBootfileName(String bootfileName) {
        this.bootfileName = bootfileName;
    }

    public String getKernelName() {
        return kernelName;
    }

    public void setKernelName(String kernelName) {
        this.kernelName = kernelName;
    }

    public String getInitrdName() {
        return initrdName;
    }

    public void setInitrdName(String initrdName) {
        this.initrdName = initrdName;
    }
}
