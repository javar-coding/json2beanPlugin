package com.project.demo.json;

import java.util.List;

/**
 * @author libo
 * @date 2020-02-19
 */
public class Node {
    private NodeType nodeType;
    /**
     * 节点名称
     */
    private String name;
    /**
     * 下划线
     */
    private String lineName;
    /**
     * 节点类名称
     */
    private String className;
    /**
     * 注释
     */
    private String annotation;
    /**
     * 子节点
     */
    private List<Node> child;
    /**
     * 节点开始行
     */
    private Integer lineStart;
    /**
     * 节点结束行
     */
    private Integer lineEnd;



    public enum NodeType {
        ARRAY,
        PLAIN,
        OBJECT,
        MAP,
        ARRAY_PLAIN_ITEM
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLineName() {
        return lineName;
    }

    public void setLineName(String lineName) {
        this.lineName = lineName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public List<Node> getChild() {
        return child;
    }

    public void setChild(List<Node> child) {
        this.child = child;
    }

    public Integer getLineStart() {
        return lineStart;
    }

    public void setLineStart(Integer lineStart) {
        this.lineStart = lineStart;
    }

    public Integer getLineEnd() {
        return lineEnd;
    }

    public void setLineEnd(Integer lineEnd) {
        this.lineEnd = lineEnd;
    }

    @Override
    public String toString() {
        return "Node{" +
                "nodeType=" + nodeType +
                ", name='" + name + '\'' +
                ", lineName='" + lineName + '\'' +
                ", className='" + className + '\'' +
                ", annotation='" + annotation + '\'' +
                ", child=" + child +
                ", lineStart=" + lineStart +
                ", lineEnd=" + lineEnd +
                '}';
    }
}
