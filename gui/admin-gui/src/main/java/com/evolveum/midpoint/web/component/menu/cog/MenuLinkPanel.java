package com.evolveum.midpoint.web.component.menu.cog;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class MenuLinkPanel extends Panel {

    private static String ID_MENU_ITEM_LINK = "menuItemLink";
    private static String ID_MENU_ITEM_LABEL = "menuItemLabel";

    public MenuLinkPanel(String id, IModel<InlineMenuItem> item) {
        super(id);

        initLayout(item);
    }

    private void initLayout(IModel<InlineMenuItem> item) {
        final InlineMenuItem dto = item.getObject();

        AbstractLink a;
        if (dto.isSubmit()) {
            a = new AjaxSubmitLink(ID_MENU_ITEM_LINK) {

                @Override
                protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                    MenuLinkPanel.this.onSubmit(target, form, dto.getAction());
                }

                @Override
                protected void onError(AjaxRequestTarget target, Form<?> form) {
                    MenuLinkPanel.this.onError(target, form, dto.getAction());
                }

                @Override
                protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                    super.updateAjaxAttributes(attributes);
                    attributes.setEventPropagation(AjaxRequestAttributes.EventPropagation.BUBBLE);
                }
            };
        } else {
            a = new AjaxLink(ID_MENU_ITEM_LINK) {

                @Override
                public void onClick(AjaxRequestTarget target) {
                    MenuLinkPanel.this.onClick(target, dto.getAction());
                }

                @Override
                protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                    super.updateAjaxAttributes(attributes);
                    attributes.setEventPropagation(AjaxRequestAttributes.EventPropagation.BUBBLE);
                }
            };
        }
        add(a);

        a.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                if (dto.getAction() == null) {
                    return false;
                }
                return true;
            }
        });

        Label span = new Label(ID_MENU_ITEM_LABEL, dto.getLabel());
        span.setRenderBodyOnly(true);
        a.add(span);
    }

    protected void onSubmit(AjaxRequestTarget target, Form<?> form, InlineMenuItemAction action) {
        if (action != null) {
            action.onSubmit(target, form);
        }
    }

    protected void onError(AjaxRequestTarget target, Form<?> form, InlineMenuItemAction action) {
        if (action != null) {
            action.onError(target, form);
        }
    }

    protected void onClick(AjaxRequestTarget target, InlineMenuItemAction action) {
        if (action != null) {
            action.onClick(target);
        }
    }
}
