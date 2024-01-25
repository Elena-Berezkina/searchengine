package searchengine.services;

import java.util.concurrent.CopyOnWriteArrayList;

public class Node {

    private String url;
    private volatile Node parent;

    private volatile CopyOnWriteArrayList<Node> children;

    public Node(String url) {
        this.url = url;
        parent = null;
        children = new CopyOnWriteArrayList<>();
    }

    public synchronized void addChild(Node element) {
        Node root = getRootElement();
        if(!root.contains(element.getUrl())) {
            element.setParent(this);
            children.add(element);
        }
    }

    private boolean contains(String url) {
        if (this.url.equals(url)) {
            return true;
        }
        for (Node child : children) {
            if(child.contains(url))
                return true;
        }

        return false;
    }

    public String getUrl() {
        return url;
    }

    private void setParent(Node n) {
        synchronized (this) {
            this.parent = n;
        }
    }

    public Node getRootElement() {

        return parent == null ? this : parent.getRootElement();
    }

    public CopyOnWriteArrayList<Node> getChildren() {
        return children;
    }

}
