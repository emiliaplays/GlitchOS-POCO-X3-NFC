"""Build original Glitch UI MIUI modules against the connected POCO's resource inventory.

No APK modification: output is a normal MTZ with resource color and image overrides.
The base icon/wallpaper prototype is preserved; generated module source is retained.
"""
from pathlib import Path
import io, json, re, zipfile
from xml.etree import ElementTree as ET
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'local/system-theme-resources'
OUT = ROOT / 'mobile/output/system-theme'
OUT.mkdir(parents=True, exist_ok=True)
NAVY = '#ff0b202b'
CARD = '#ff183943'
MINT = '#ff8bffe0'
WHITE = '#fff9fafc'
MUTED = '#ffb9cdd6'
INK = '#ff102d3b'
DISABLED = '#ff66838d'

def inventory(part):
    text = (RES / f'{part}-resources.txt').read_text(encoding='utf-8-sig')
    return {kind: set(re.findall(r'spec resource \S+ [^: ]+:' + kind + r'/(\w+)', text))
            for kind in ['color', 'drawable', 'dimen']}

def xml_values(colors):
    root = ET.Element('MIUI_Theme_Values')
    for name, value in sorted(colors.items()):
        ET.SubElement(root, 'color', name=name).text = value
    return ET.tostring(root, encoding='utf-8', xml_declaration=True)

def patch_png(fill, stroke=False):
    # Source nine-patch: transparent border, black stretch and content markers.
    scale = 4
    image = Image.new('RGBA', (64*scale, 64*scale))
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((0, 0, 64*scale-1, 64*scale-1), radius=18*scale,
                           fill=fill, outline='#8bffe049' if stroke else None, width=scale)
    image = image.resize((64, 64), Image.Resampling.LANCZOS)
    result = Image.new('RGBA', (66, 66))
    result.paste(image, (1, 1))
    draw = ImageDraw.Draw(result)
    draw.line((32, 0, 33, 0), fill='black')
    draw.line((0, 32, 0, 33), fill='black')
    draw.line((17, 65, 48, 65), fill='black')
    draw.line((65, 17, 65, 48), fill='black')
    out = io.BytesIO(); result.save(out, format='PNG')
    return out.getvalue()

def settings_icon(name):
    """Original line glyphs for Settings entries, drawn rather than extracted."""
    im = Image.new('RGBA', (192,192)); d = ImageDraw.Draw(im)
    d.rounded_rectangle((3,3,188,188), radius=46, fill='#183943', outline='#8bffe0', width=3)
    c='#8bffe0'; w=8
    def line(points): d.line(points,fill=c,width=w,joint='curve')
    if 'wifi' in name or 'wlan' in name:
        for box in [(38,48,154,166),(57,69,135,147),(76,91,116,131)]:d.arc(box,218,322,fill=c,width=w)
        d.ellipse((89,125,103,139),fill=c)
    elif 'bluetooth' in name:
        line([(92,43),(126,73),(65,120),(92,145),(92,43),(126,115),(65,73)])
    elif any(s in name for s in ['my_device','dualsim','tablet']):
        d.rounded_rectangle((64,38,128,154),radius=12,outline=c,width=w);line([(84,138),(108,138)])
    elif any(s in name for s in ['security','privacy','vpn']):
        line([(96,39),(142,56),(137,110),(119,136),(96,154),(73,136),(55,110),(50,56),(96,39)])
        line([(75,94),(91,110),(120,78)])
    elif 'battery' in name:
        d.rounded_rectangle((42,66,145,126),radius=8,outline=c,width=w)
        line([(151,84),(151,108)]);d.rectangle((56,79,103,114),fill=c)
    elif any(s in name for s in ['account','users','personal']):
        d.ellipse((76,42,116,82),outline=c,width=w);d.arc((53,92,139,171),180,360,fill=c,width=w)
    elif 'sound' in name:
        line([(54,82),(75,82),(106,58),(106,135),(75,111),(54,111),(54,82)])
        d.arc((92,59,150,135),285,75,fill=c,width=w)
    elif any(s in name for s in ['wallpaper','display']):
        d.rounded_rectangle((42,49,150,141),radius=13,outline=c,width=w)
        line([(50,129),(81,91),(105,116),(124,96),(142,128)]);d.ellipse((111,67,126,82),fill=c)
    elif any(s in name for s in ['notification','app_timer']):
        d.arc((59,47,133,129),180,360,fill=c,width=w)
        line([(59,84),(59,119),(48,130),(144,130),(133,119),(133,84)])
        line([(85,147),(107,147)])
    elif 'location' in name:
        d.ellipse((58,40,134,116),outline=c,width=w);line([(64,103),(96,155),(128,103)])
        d.ellipse((84,66,108,90),outline=c,width=6)
    elif 'home' in name:
        line([(41,89),(96,44),(151,89),(137,89),(137,148),(106,148),(106,111),(85,111),(85,148),(55,148),(55,89)])
    elif 'updater' in name:
        line([(96,147),(96,48),(59,85),(96,48),(133,85)]);line([(57,147),(135,147)])
    else:
        # Shared settings/device-services gear glyph; entry labels retain meaning.
        import math
        d.ellipse((59,59,133,133),outline=c,width=w);d.ellipse((83,83,109,109),outline=c,width=6)
        for a in range(0,360,45):
            r=math.radians(a);line([(96+39*math.cos(r),96+39*math.sin(r)),(96+56*math.cos(r),96+56*math.sin(r))])
    out=io.BytesIO();im.save(out,format='PNG');return out.getvalue()

def colors_for(part, names):
    values = {}
    def put(name, value):
        if name in names: values[name] = value
    # Explicit contracts resolved from this device, not copied from another theme.
    common = {
        'accent_material_dark': MINT, 'accent_material_light': MINT,
        'preference_fallback_accent_color': MINT,
        'background_material_dark': NAVY, 'background_material_light': NAVY,
        'background_floating_material_dark': CARD, 'background_floating_material_light': CARD,
        'screen_background_dark': NAVY, 'screen_background_holo_dark': NAVY,
        'main_page_background': NAVY, 'window_background_color': NAVY,
        'miuix_appcompat_background_dark': NAVY, 'miuix_appcompat_background_light': NAVY,
        'miuix_appcompat_floating_window_bg_color_dark': CARD,
        'miuix_appcompat_floating_window_bg_color_light': CARD,
        'miuix_appcompat_accent_color_dark': MINT, 'miuix_appcompat_accent_color_light': MINT,
    }
    for n, v in common.items(): put(n, v)
    if part in ['settings','framework','miui']:
        put('black', NAVY)
    for n in names:
        if n.startswith('miuix_appcompat_') or n.startswith('miuix_preference_'):
            if any(x in n for x in ['primary_color_', 'highlight_color', 'progress_primary_color']):
                put(n, MINT)
            elif any(x in n for x in ['window_background', 'action_bar_background', 'list_item_bg_color']):
                put(n, CARD if 'item' in n else NAVY)
            elif any(x in n for x in ['primary_text', 'text_color_primary']): put(n, WHITE)
            elif any(x in n for x in ['secondary_text', 'right_text_color']): put(n, MUTED)
            elif 'category_text_color' in n: put(n, MINT)
            elif 'arrow_right_normal_stroke' in n: put(n, MUTED)
    if part == 'systemui':
        for n in names:
            if re.match(r'qs_control_big_tile_(?:[0-4]|blue|green|orange|white)_bg$', n): put(n, MINT)
            elif re.match(r'qs_control_big_tile_[0-4]_bg_(?:inactive|unavailable)$', n): put(n, CARD)
            elif n in ['qs_control_big_tile_title_color', 'qs_control_big_tile_sub_title_color',
                       'qs_control_tile_icon_enable_color', 'cc_qs_tile_icon_color', 'qs_tile_icon_enabled_color']:
                put(n, INK)
            elif n.startswith('qs_control_') and any(x in n for x in ['title_color_off', 'header_clock_color', 'tile_label_text_color', 'expand_item_title_color', 'expand_tile_title_color']): put(n, WHITE)
            elif n.startswith('qs_control_') and any(x in n for x in ['sub_title_color_off', 'header_date_color', 'footer_text_color']): put(n, MUTED)
        mapping = {
            'cc_qs_tile_icon_bg_color_active': MINT,
            'cc_qs_tile_icon_bg_color_active_auto_brightness': MINT,
            'cc_qs_tile_icon_bg_color_active_battery_related': MINT,
            'cc_qs_tile_icon_color_off': MUTED,
            'qs_control_tile_icon_disabled_color': MUTED,
            'qs_control_tile_icon_unavailable_color': DISABLED,
            'qs_control_tile_text_unavailable_color': DISABLED,
            'qs_control_brightness_progress_color': MINT,
            'qs_control_brightness_secondary_progress_color': CARD,
            'qs_control_brightness_low_color': INK,
            'qs_panel_background_color': NAVY,
            'qs_tile_icon_bg_enabled_color': MINT, 'qs_tile_icon_bg_disabled_color': CARD,
            'qs_tile_icon_disabled_color': MUTED, 'qs_tile_label_text_color': WHITE,
            'qs_tile_label_text_color_secondary': MUTED,
            'qs_control_detail_bg_color': NAVY, 'qs_control_detail_layout_bg_color': CARD,
            'qs_control_detail_more_button_bg_color': CARD,
            'qs_control_detail_more_button_color': MINT,
            'qs_control_detail_selected_color': MINT,
            'qs_control_detail_unselected_color': MUTED,
            'qs_control_detail_header_text_color_plugin': WHITE,
            'qs_control_header_edit_color': MINT, 'qs_control_header_settings_color': MINT,
            'notification_bg_color': CARD, 'notification_heads_up_bg_color': CARD,
            'notification_legacy_background_color': CARD,
            'notification_material_background_dark_color': CARD,
            'notification_material_background_media_default_color': CARD,
            'notification_primary_text_color': WHITE, 'notification_primary_text_color_light': WHITE,
            'notification_secondary_text_color_light': MUTED,
            'notification_time_color': MUTED, 'notification_action_text_color': MINT,
            'notification_action_bg_color': NAVY, 'notification_divider_color': '#ff2d515c',
            'notification_progress_horizontal_progress_color': MINT,
            'notification_progress_horizontal_bg_color': NAVY,
            'notification_media_seekbar_progress_color': MINT,
            'notification_media_seekbar_bg_color': NAVY,
            'notification_panel_date_color': MUTED, 'notification_panel_time_color': WHITE,
            'notification_panel_manage_color': MINT,
            'notification_section_header_label_color': MUTED,
            'notification_section_clear_all_btn_color': MINT,
            'volume_dialog_background_color': CARD,
            'volume_dialog_background_color_above_blur': CARD,
            'miui_default_lock_screen_notification_item_title_text_color': WHITE,
            'miui_default_lock_screen_notification_item_msg_text_color': MUTED,
            'miui_default_lock_screen_notification_item_time_text_color': MUTED,
        }
        for n,v in mapping.items(): put(n,v)
    if part == 'plugin':
        mapping = {
            'qs_enabled_color': MINT, 'qs_disabled_color': CARD,
            'qs_restrict_color': CARD, 'qs_unavailable_color': '#ff142d36',
            'qs_icon_enabled_color': INK, 'qs_icon_disabled_color': MUTED,
            'qs_icon_restrict_color': MUTED, 'qs_icon_unavailable_color': DISABLED,
            'qs_text_enabled_color': WHITE, 'qs_text_disabled_color': MUTED,
            'qs_text_unavailable_color': DISABLED,
            'qs_card_enabled_color': MINT, 'qs_card_cellular_color': MINT,
            'qs_card_flashlight_color': MINT, 'qs_card_disabled_color': CARD,
            'qs_card_restricted_color': CARD, 'qs_card_unavailable_color': '#ff142d36',
            'qs_card_primary_text_enabled_color': INK,
            'qs_card_secondary_text_enabled_color': INK,
            'qs_card_primary_text_disabled_color': WHITE,
            'qs_card_secondary_text_disabled_color': MUTED,
            'qs_card_primary_text_restricted_color': MUTED,
            'qs_card_secondary_text_restricted_color': MUTED,
            'qs_card_primary_text_unavailable_color': DISABLED,
            'qs_card_secondary_text_unavailable_color': DISABLED,
            'qs_card_expand_indicator_color': INK,
            'toggle_slider_progress_color': MINT,
            'toggle_slider_progress_background_color': CARD,
            'toggle_slider_icon_color': INK, 'slider_bg_color': CARD,
            'detail_panel_background_color': NAVY, 'detail_panel_button_background_color': CARD,
            'detail_panel_button_text_color': MINT, 'detail_panel_header_text_color': WHITE,
            'header_clock_text_color': WHITE, 'header_date_text_color': MUTED,
            'header_settings_icon_color': MINT, 'header_customize_icon_color': MINT,
            'customize_panel_save_button_background_color': MINT,
            'customize_panel_save_button_text_color': INK,
            'customize_panel_not_added_background_color': CARD,
            'customize_panel_not_added_text_color': MUTED,
            'external_entry_background_color': CARD,
            'external_entry_icon_color': MINT, 'external_entry_primary_text_color': WHITE,
            'external_entry_secondary_text_color': MUTED,
            'control_center_universal_enabled_color': MINT,
            'control_center_universal_disabled_color': CARD,
            'control_center_universal_icon_color': INK,
            'control_center_universal_primary_text_color': WHITE,
            'control_center_universal_secondary_text_color': MUTED,
            'control_center_universal_text_disabled_color': DISABLED,
            'miui_volume_bg_color': CARD, 'miui_volume_bg_color_collapse': CARD,
            'miui_volume_color_accent': MINT, 'miui_volume_color_btn_seleted': MINT,
            'miui_volume_dragging_idle_color': MINT,
            'miui_volume_disabled_color': DISABLED, 'miui_volume_disabled_color_collapse': DISABLED,
            'miui_volume_dnd_popup_close_text_color': MINT,
            'miui_volume_dnd_popup_text_color': WHITE, 'miui_volume_dnd_text_color': MUTED,
            'miui_volume_ringer_status_color': MUTED, 'miui_volume_text_color': WHITE,
            'miui_volume_expand_button_color': MINT,
            'miui_volume_expand_button_color_blur': MINT,
            'miui_volume_expand_button_color_blur_light': MINT,
            'miui_volume_tint_dark': INK, 'miui_volume_tint_light': MINT,
            'volume_dialog_bg_color': CARD,
            'miplay_volume_bar_bg_color': CARD, 'miplay_volume_bar_primary_color': MINT,
        }
        for n,v in mapping.items(): put(n,v)
    if part == 'miui':
        for n in names:
            if n in ['background_dark', 'immersion_window_background_dark']: put(n, NAVY)
            elif re.match(r'(?:primary_color|progressPrimaryColor|progressbar_color|seekbar_progress_color|highlight_normal)', n): put(n, MINT)
            elif re.match(r'(?:primary_text|preference_primary_text).*dark', n): put(n, WHITE)
            elif re.match(r'(?:secondary_text|preference_secondary_text).*dark', n): put(n, MUTED)
            elif re.match(r'(?:list_item_bg_color|dropdown_popup_list_item_bg).*dark', n): put(n, CARD)
    if part == 'poco':
        for n,v in {
            'recents_task_bar_default_background_color': CARD,
            'recents_task_view_default_background_color': NAVY,
            'recents_dock_area_text_color': WHITE, 'recents_dock_btn_text_color': MINT,
            'recents_tip_color': MUTED, 'recents_tv_small_window_text_color': WHITE,
            'task_manager_no_recents_text': MUTED,
        }.items(): put(n,v)
    return values

modules = {}
report = {}
for part, package in [('systemui','com.android.systemui'), ('plugin','miui.systemui.plugin'),
                      ('settings','com.android.settings'), ('framework','framework-res'),
                      ('miui','framework-miui-res'), ('poco','com.miui.home'),
                      ('poco','com.mi.android.globallauncher')]:
    inv = inventory(part)
    values = colors_for(part, inv['color'])
    files = {'theme_values.xml': xml_values(values), 'nightmode/theme_values.xml': xml_values(values)}
    if part == 'systemui':
        for name, color in [('ic_cc_qs_bg_active_normal', '#8bffe0'),
                            ('ic_cc_qs_bg_active_auto_brightness', '#8bffe0'),
                            ('ic_cc_qs_bg_active_battery_related', '#8bffe0'),
                            ('ic_cc_qs_bg_inactive', '#183943'),
                            ('ic_cc_qs_bg_unavailable', '#142d36'),
                            ('notification_bg_normal', '#183943'),
                            ('notification_bg_low_normal', '#183943'),
                            ('notification_bg_normal_pressed', '#254b55'),
                            ('notification_bg_low_pressed', '#254b55')]:
            if name in inv['drawable']:
                for prefix in ['', 'nightmode/']:
                    files[f'{prefix}res/drawable-xxhdpi/{name}.9.png'] = patch_png(color, 'inactive' in name or 'notification' in name)
    if part == 'settings':
        headers=(RES/'settings-headers.txt').read_text(encoding='utf-8-sig')
        text=(RES/'settings-resources.txt').read_text(encoding='utf-8-sig')
        ids=dict(re.findall(r'spec resource (0x\w+) [^: ]+:drawable/(\w+)',text))
        names={ids[i] for i in re.findall(r'android:icon[^\n]*?@(0x[0-9a-f]+)',headers) if i in ids and ids[i].startswith('ic_')}
        for name in names:
            for prefix in ['', 'nightmode/']:
                files[f'{prefix}res/drawable-xxhdpi/{name}.png']=settings_icon(name)
    source = OUT/'modules'/package
    for path, data in files.items():
        dest = source/path; dest.parent.mkdir(parents=True,exist_ok=True); dest.write_bytes(data)
    archive = io.BytesIO()
    with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
        for path,data in files.items(): z.writestr(path,data)
    modules[package] = archive.getvalue()
    report[package] = {'colors':len(values),'assets':len(files)-2}

base = ROOT/'mobile/output/theme/theme/GlitchUI-Poco.mtz'
dest = OUT/'GlitchUI-System-1.2.0.mtz'
with zipfile.ZipFile(base) as old, zipfile.ZipFile(dest,'w',zipfile.ZIP_DEFLATED) as z:
    for n in old.namelist():
        data = old.read(n)
        if n == 'description.xml':
            xml = ET.fromstring(data)
            xml.find('title').text = 'Glitch UI'
            xml.find('version').text = '1.2.0'
            xml.find('designer').text = 'glitchbyemilia'
            xml.find('description').text = 'Original navy and mint wallpaper, icons and MIUI system color modules. POCO X3 NFC development build.'
            data = ET.tostring(xml,encoding='utf-8',xml_declaration=True)
        z.writestr(n,data)
    for n,data in modules.items(): z.writestr(n,data)
(OUT/'build-report.json').write_text(json.dumps(report,indent=2))
asset = ROOT/'mobile/assets/theme-modules.zip'
asset.parent.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(asset, 'w', zipfile.ZIP_DEFLATED) as z:
    for name, data in modules.items(): z.writestr(name, data)
print(dest)
print(json.dumps(report,indent=2))
