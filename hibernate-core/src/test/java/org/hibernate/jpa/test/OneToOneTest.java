package org.hibernate.jpa.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OneToOneTest extends BaseEntityManagerFunctionalTestCase {

  private EntityManager entityManager;

  @Before
  public void setUp() throws Exception {
    entityManager = createEntityManager();
  }

  @After
  public void tearDown() throws Exception {
    entityManager.close();
  }

  @Test
  public void shouldNotThrowForeignKeyConstraintException() {
    Child child = new Child();
    Parent parent = new Parent(new ParentId("SOME_PARENT"), child);

    parent.addChild(new Child());

    entityManager.getTransaction().begin();
    entityManager.persist(parent);
    entityManager.getTransaction().commit();

    Parent parentLoaded = entityManager.find(Parent.class, parent.getId());

    assertThat(parentLoaded.getChildren()).doesNotContain(child);
  }

  @Override
  protected Class<?>[] getAnnotatedClasses() {
    return new Class[]{Parent.class, Child.class, ChildId.class, ParentId.class};
  }

  @Entity
  static class Parent {

    @EmbeddedId
    @AttributeOverride(name = "value", column = @Column(name = "id"))
    private final ParentId id;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "parent_id", referencedColumnName = "id")
    private final Collection<Child> children = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumns({
        @JoinColumn(name = "id", referencedColumnName = "parent_id", insertable = false, updatable = false),
        @JoinColumn(name = "child_id", referencedColumnName = "id", insertable = false, updatable = false),
    })
    private final Child child;

    public Parent(ParentId id) {
      this(id, null);
    }

    public Parent(ParentId id, Child child) {
      this.id = id;

      if(child != null) {
        child.setChildId(getNextChildId());
      }

      this.child = child;
    }

    protected Parent() {
      id = null;
      child = null;
    }

    public ParentId getId() {
      return id;
    }

    public void addChild(Child child) {
      child.setChildId(getNextChildId());

      children.add(child);
    }

    private ChildId getNextChildId() {
      if (child != null) {
        return new ChildId(children.size() + 2, id);
      }

      return new ChildId(children.size() + 1, id);
    }

    public Collection<Child> getChildren() {
      return children;
    }

    public Child getChild() {
      return child;
    }
  }

  @Entity
  static class Child {

    @EmbeddedId
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "id")),
        @AttributeOverride(name = "parentId.value", column = @Column(name = "parent_id"))
    })
    private ChildId childId;

    public ChildId getChildId() {
      return childId;
    }

    public void setChildId(ChildId childId) {
      this.childId = childId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Child)) {
        return false;
      }

      Child child = (Child) o;

      return childId != null ? childId.equals(child.childId) : child.childId == null;
    }

    @Override
    public int hashCode() {
      return childId != null ? childId.hashCode() : 0;
    }
  }

  @Embeddable
  static class ChildId implements Serializable {

    private final Integer id;

    private final ParentId parentId;

    public ChildId(Integer id, ParentId parentId) {
      this.id = id;
      this.parentId = parentId;
    }

    protected ChildId() {
      id = null;
      parentId = null;
    }

    public Integer getId() {
      return id;
    }

    public ParentId getParentId() {
      return parentId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof ChildId)) {
        return false;
      }

      ChildId childId = (ChildId) o;

      if (id != null ? !id.equals(childId.id) : childId.id != null) {
        return false;
      }
      return parentId != null ? parentId.equals(childId.parentId) : childId.parentId == null;
    }

    @Override
    public int hashCode() {
      int result = id != null ? id.hashCode() : 0;
      result = 31 * result + (parentId != null ? parentId.hashCode() : 0);
      return result;
    }
  }

  @Embeddable
  static class ParentId implements Serializable {

    private final String value;

    public ParentId(String value) {
      this.value = value;
    }

    protected ParentId() {
      value = null;
    }

    public String getValue() {
      return value;
    }
  }
}
