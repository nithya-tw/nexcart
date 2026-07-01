#!/usr/bin/env python3
"""
Generate a custom JaCoCo aggregate HTML report for NexCart
"""
import re
from pathlib import Path
from datetime import datetime

def parse_coverage_from_html(html_path):
    """Parse coverage data from JaCoCo HTML report"""
    with open(html_path, 'r') as f:
        content = f.read()
    
    # Find the Total row in tfoot
    tfoot_match = re.search(r'<tfoot>.*?</tfoot>', content, re.DOTALL)
    if not tfoot_match:
        return None
    
    tfoot = tfoot_match.group()
    
    # Extract all data cells
    cells = re.findall(r'<td[^>]*>(.*?)</td>', tfoot, re.DOTALL)
    
    if len(cells) >= 5:
        return {
            'missed_instructions': cells[1].strip(),
            'instruction_coverage': cells[2].strip(),
            'missed_branches': cells[3].strip(),
            'branch_coverage': cells[4].strip()
        }
    return None

# Services to analyze
services = [
    ('user-service', 'User Service'),
    ('product-service', 'Product Service'),
    ('cart-service', 'Cart Service'),
    ('inventory-service', 'Inventory Service'),
    ('order-service', 'Order Service'),
    ('auth-service', 'Auth Service')
]

base_path = Path('/Users/nithyamukundan/Documents/bootcamp/nexcart/services')

service_data = []
total_instructions_missed = 0
total_instructions_covered = 0
total_branches_missed = 0
total_branches_covered = 0

for service_dir, service_name in services:
    html_path = base_path / service_dir / 'target/site/jacoco/index.html'
    
    if html_path.exists():
        data = parse_coverage_from_html(html_path)
        if data:
            service_data.append({
                'name': service_name,
                'dir': service_dir,
                **data
            })
            
            # Parse totals from "1,234 of 5,678" format
            inst_parts = data['missed_instructions'].replace(',', '').split(' of ')
            if len(inst_parts) == 2:
                missed = int(inst_parts[0])
                total = int(inst_parts[1])
                total_instructions_missed += missed
                total_instructions_covered += (total - missed)
            
            branch_parts = data['missed_branches'].replace(',', '').split(' of ')
            if len(branch_parts) == 2:
                missed = int(branch_parts[0])
                total = int(branch_parts[1])
                total_branches_missed += missed
                total_branches_covered += (total - missed)

# Calculate overall coverage
inst_total = total_instructions_missed + total_instructions_covered
branch_total = total_branches_missed + total_branches_covered

inst_coverage_pct = (total_instructions_covered / inst_total * 100) if inst_total > 0 else 0
branch_coverage_pct = (total_branches_covered / branch_total * 100) if branch_total > 0 else 0

# Generate HTML report
html = f"""<?xml version="1.0" encoding="UTF-8"?><!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en">
<head>
    <meta http-equiv="Content-Type" content="text/html;charset=UTF-8"/>
    <title>NexCart Aggregate Coverage Report</title>
    <style>
        body {{ font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }}
        .header {{ background: #2c3e50; color: white; padding: 20px; border-radius: 5px; margin-bottom: 20px; }}
        .header h1 {{ margin: 0; font-size: 28px; }}
        .header .subtitle {{ opacity: 0.8; margin-top: 5px; }}
        table {{ width: 100%; border-collapse: collapse; background: white; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }}
        th {{ background: #34495e; color: white; padding: 12px; text-align: left; font-weight: bold; }}
        td {{ padding: 10px 12px; border-bottom: 1px solid #ecf0f1; }}
        tr:hover {{ background: #f8f9fa; }}
        .bar {{ width: 200px; }}
        .ctr2 {{ text-align: right; font-weight: bold; }}
        .green {{ color: #27ae60; }}
        .yellow {{ color: #f39c12; }}
        .red {{ color: #e74c3c; }}
        tfoot {{ background: #ecf0f1; font-weight: bold; font-size: 1.1em; }}
        tfoot td {{ border-top: 2px solid #34495e; padding: 15px 12px; }}
        .footer {{ margin-top: 20px; text-align: center; color: #7f8c8d; }}
        .stats {{ display: flex; justify-content: space-around; margin: 20px 0; }}
        .stat-card {{ background: white; padding: 20px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); text-align: center; flex: 1; margin: 0 10px; }}
        .stat-value {{ font-size: 36px; font-weight: bold; color: #27ae60; }}
        .stat-label {{ color: #7f8c8d; margin-top: 5px; }}
        a {{ color: #3498db; text-decoration: none; }}
        a:hover {{ text-decoration: underline; }}
    </style>
</head>
<body>
    <div class="header">
        <h1>NexCart Aggregate Coverage Report</h1>
        <div class="subtitle">Generated on {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}</div>
    </div>
    
    <div class="stats">
        <div class="stat-card">
            <div class="stat-value">{inst_coverage_pct:.1f}%</div>
            <div class="stat-label">Instruction Coverage</div>
        </div>
        <div class="stat-card">
            <div class="stat-value">{branch_coverage_pct:.1f}%</div>
            <div class="stat-label">Branch Coverage</div>
        </div>
        <div class="stat-card">
            <div class="stat-value">{(inst_coverage_pct + branch_coverage_pct) / 2:.1f}%</div>
            <div class="stat-label">Overall Average</div>
        </div>
    </div>
    
    <table>
        <thead>
            <tr>
                <th>Service</th>
                <th class="bar">Missed Instructions</th>
                <th class="ctr2">Coverage</th>
                <th class="bar">Missed Branches</th>
                <th class="ctr2">Coverage</th>
            </tr>
        </thead>
        <tbody>
"""

for service in service_data:
    inst_cov = service['instruction_coverage']
    branch_cov = service['branch_coverage']
    
    # Color based on coverage
    inst_class = 'green' if inst_cov.replace('%', '').isdigit() and int(inst_cov.replace('%', '')) >= 80 else 'yellow'
    branch_class = 'green' if branch_cov.replace('%', '').isdigit() and int(branch_cov.replace('%', '')) >= 80 else 'yellow'
    
    html += f"""            <tr>
                <td><a href="../../../services/{service['dir']}/target/site/jacoco/index.html">{service['name']}</a></td>
                <td class="bar">{service['missed_instructions']}</td>
                <td class="ctr2 {inst_class}">{inst_cov}</td>
                <td class="bar">{service['missed_branches']}</td>
                <td class="ctr2 {branch_class}">{branch_cov}</td>
            </tr>
"""

html += f"""        </tbody>
        <tfoot>
            <tr>
                <td>Total</td>
                <td class="bar">{total_instructions_missed:,} of {inst_total:,}</td>
                <td class="ctr2 green">{inst_coverage_pct:.0f}%</td>
                <td class="bar">{total_branches_missed:,} of {branch_total:,}</td>
                <td class="ctr2 green">{branch_coverage_pct:.0f}%</td>
            </tr>
        </tfoot>
    </table>
    
    <div class="footer">
        <p>NexCart E-Commerce Platform | 6 Services | 360 Tests</p>
        <p>Created with JaCoCo 0.8.11</p>
    </div>
</body>
</html>
"""

# Write the report
output_path = Path('/Users/nithyamukundan/Documents/bootcamp/nexcart/jacoco-aggregate/target/site/jacoco-aggregate')
output_path.mkdir(parents=True, exist_ok=True)

with open(output_path / 'index.html', 'w') as f:
    f.write(html)

print(f"\n✅ Custom aggregate report generated!")
print(f"📁 Location: {output_path}/index.html")
print(f"\n📊 Coverage Summary:")
print(f"   Instruction Coverage: {inst_coverage_pct:.1f}%")
print(f"   Branch Coverage: {branch_coverage_pct:.1f}%")
print(f"   Overall Average: {(inst_coverage_pct + branch_coverage_pct) / 2:.1f}%")
