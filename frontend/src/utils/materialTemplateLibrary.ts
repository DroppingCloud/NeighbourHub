export interface BuiltInMaterialTemplate {
  key: string
  fileName: string
  title: string
  description: string
  contentHtml: string
}

function table(rows: Array<[string, string]>) {
  return `
    <table>
      <tbody>
        ${rows.map(([label, value]) => `<tr><th>${label}</th><td>${value}</td></tr>`).join('')}
      </tbody>
    </table>
  `
}

function signature(label = '申请人签字') {
  return `
    <div class="signature">
      <p>${label}: ____________________</p>
      <p>日期: ________年____月____日</p>
    </div>
  `
}

function note(text: string) {
  return `<p class="note">${text}</p>`
}

export const builtInMaterialTemplates: BuiltInMaterialTemplate[] = [
  {
    key: 'house-rental-contract',
    fileName: '房屋租赁合同模板.docx',
    title: '房屋租赁合同',
    description: '用于居住证办理中“合法稳定住所”的租赁住房证明。',
    contentHtml: `
      <h1>房屋租赁合同</h1>
      ${table([
        ['出租方（甲方）', '王某某'],
        ['甲方身份证号', '110101198512121234'],
        ['承租方（乙方）', '张三'],
        ['乙方身份证号', '11010119900307663X'],
        ['房屋坐落', 'XX市XX区XX街道XX小区2号楼202室'],
        ['租赁期限', '自2026年1月1日起至2026年12月31日止'],
        ['月租金', '人民币3500元']
      ])}
      ${signature('甲方签字')}
      ${signature('乙方签字')}
      ${note('填写说明：请上传包含双方姓名、身份证号、房屋地址、租赁期限、签字日期的关键页。')}
    `
  },
  {
    key: 'real-estate-certificate',
    fileName: '房屋产权证明文件模板.docx',
    title: '房屋产权证明文件',
    description: '用于居住证办理或居住证明开具中的自有住房证明。',
    contentHtml: `
      <h1>房屋产权证明文件</h1>
      ${table([
        ['证书编号', 'XX不动产权第0000001号'],
        ['权利人', '张某某'],
        ['坐落', 'XX市XX区XX街道XX小区1号楼101室'],
        ['权利类型', '国有建设用地使用权/房屋所有权'],
        ['用途', '住宅'],
        ['面积', '房屋建筑面积89.50平方米']
      ])}
      ${note('填写说明：请上传权利人、坐落、用途、面积等信息清晰可见的页面。')}
    `
  },
  {
    key: 'purchase-contract',
    fileName: '购房合同模板.docx',
    title: '购房合同',
    description: '用于尚未取得产权证但已签订购房合同的居住证明场景。',
    contentHtml: `
      <h1>购房合同</h1>
      ${table([
        ['出卖人', 'XX房地产开发有限公司'],
        ['买受人', '张某某'],
        ['身份证号', '11010119900307663X'],
        ['房屋坐落', 'XX市XX区XX街道XX小区1号楼101室'],
        ['建筑面积', '89.50平方米'],
        ['约定交付日期', '2026年12月31日']
      ])}
      ${signature('买受人签字')}
      ${note('填写说明：请上传买受人、身份证号、房屋坐落、面积、交付日期等关键页。')}
    `
  },
  {
    key: 'unit-accommodation-proof',
    fileName: '单位或学校住宿证明模板.docx',
    title: '用人单位/就读学校出具的住宿证明',
    description: '用于居住在单位宿舍或学校宿舍时证明实际居住地址。',
    contentHtml: `
      <h1>住宿证明</h1>
      <p>兹证明 <strong>张三</strong>（身份证号：<strong>11010119900307663X</strong>）现居住于本单位/学校提供的宿舍。</p>
      <p>宿舍地址：XX市XX区XX街道XX宿舍楼B座301室。</p>
      <p>居住起始时间：2026年1月1日。</p>
      <p>特此证明。</p>
      <p class="seal">单位/学校名称（盖章）：____________________</p>
      <p class="seal">联系人及电话：____________________</p>
      <p class="seal">日期：______年____月____日</p>
      ${note('填写说明：需写明住宿人、身份证号、宿舍地址、居住起始时间，并加盖单位或学校公章。')}
    `
  },
  {
    key: 'business-license',
    fileName: '工商营业执照模板.docx',
    title: '工商营业执照',
    description: '用于居住证办理中“合法稳定就业”的经营证明。',
    contentHtml: `
      <h1>工商营业执照</h1>
      ${table([
        ['统一社会信用代码', '91110105MA00XXXXXX'],
        ['名称', 'XX便民服务有限公司'],
        ['类型', '有限责任公司'],
        ['法定代表人/经营者', '张三'],
        ['住所/经营场所', 'XX市XX区XX街道XX号'],
        ['成立日期', '2026年1月1日'],
        ['经营范围', '便民服务、技术咨询、社区服务等']
      ])}
      ${note('填写说明：请上传营业执照副本或电子营业执照截图，需包含名称、住所、经营者等信息。')}
    `
  },
  {
    key: 'labor-contract',
    fileName: '劳动合同模板.docx',
    title: '劳动合同',
    description: '用于居住证办理中“合法稳定就业”的劳动关系证明。',
    contentHtml: `
      <h1>劳动合同</h1>
      ${table([
        ['用人单位（甲方）', 'XX社区服务有限公司'],
        ['劳动者（乙方）', '张三'],
        ['身份证号', '11010119900307663X'],
        ['合同期限', '2026年1月1日至2028年12月31日'],
        ['工作岗位', '社区服务专员'],
        ['工作地点', 'XX市XX区XX街道']
      ])}
      ${signature('甲方盖章')}
      ${signature('乙方签字')}
      ${note('填写说明：请上传用人单位、劳动者、合同期限、工作地点和签章清晰的关键页。')}
    `
  },
  {
    key: 'labor-relation-proof',
    fileName: '劳动关系证明模板.docx',
    title: '用人单位出具的劳动关系证明',
    description: '用于不便提供完整劳动合同时，由用人单位证明劳动关系。',
    contentHtml: `
      <h1>劳动关系证明</h1>
      <p>兹证明 <strong>张三</strong>（身份证号：<strong>11010119900307663X</strong>）系我单位员工。</p>
      <p>入职日期：2026年1月1日。</p>
      <p>工作岗位：社区服务专员。</p>
      <p>工作地点：XX市XX区XX街道。</p>
      <p class="seal">用人单位名称（盖章）：____________________</p>
      <p class="seal">联系人及电话：____________________</p>
      <p class="seal">日期：______年____月____日</p>
    `
  },
  {
    key: 'student-card',
    fileName: '学生证要求说明.docx',
    title: '学生证',
    description: '用于居住证办理中“连续就读”的就读证明。',
    contentHtml: `
      <h1>学生证材料要求</h1>
      <ol>
        <li>上传学生证个人信息页。</li>
        <li>上传当前学期注册页或学校有效注册记录。</li>
        <li>姓名、学校、学号、专业、入学时间需清晰可见。</li>
      </ol>
      ${note('说明：学生证为官方证件，一般无需填写模板，按要求上传清晰照片或扫描件即可。')}
    `
  },
  {
    key: 'continuous-study-proof',
    fileName: '连续就读证明模板.docx',
    title: '就读学校出具的连续就读证明',
    description: '用于居住证办理中“连续就读”的学校证明。',
    contentHtml: `
      <h1>连续就读证明</h1>
      <p>兹证明 <strong>张三</strong>（身份证号：<strong>11010119900307663X</strong>）系我校在读学生。</p>
      ${table([
        ['学校名称', 'XX学校'],
        ['专业/班级', 'XX专业/XX班'],
        ['学号', '2026001234'],
        ['入学时间', '2026年9月1日'],
        ['就读状态', '自入学以来连续就读']
      ])}
      <p class="seal">学校名称（盖章）：____________________</p>
      <p class="seal">日期：______年____月____日</p>
    `
  },
  {
    key: 'senior-allowance-application',
    fileName: '高龄津贴申请表.docx',
    title: '高龄津贴申请表',
    description: '用于老年补贴申请，填写申请人和补贴发放账户信息。',
    contentHtml: `
      <h1>高龄津贴申请表</h1>
      ${table([
        ['姓名', '张某某'],
        ['性别', '男'],
        ['出生日期', '1945年1月1日'],
        ['身份证号', '110101194501011234'],
        ['户籍地址', 'XX市XX区XX街道XX小区'],
        ['实际住址', 'XX市XX区XX街道XX小区'],
        ['联系电话', '13900000000'],
        ['社保卡/银行卡号', '6217000000000000000'],
        ['开户银行', 'XX银行XX支行'],
        ['紧急联系人', '张三/子女/13800000000']
      ])}
      <p>本人承诺以上信息真实有效，并同意接受相关部门核查。</p>
      ${signature('申请人/代理人签字')}
    `
  },
  {
    key: 'photo-requirement',
    fileName: '证件照要求说明.docx',
    title: '近期免冠两寸照片要求说明',
    description: '用于老年补贴申请中的近期免冠两寸照片。',
    contentHtml: `
      <h1>近期免冠两寸照片要求说明</h1>
      <ol>
        <li>近期6个月内免冠彩色两寸照片。</li>
        <li>背景建议为白色或淡蓝色。</li>
        <li>照片需清晰、无遮挡、无明显反光。</li>
        <li>可上传纸质照片扫描件或电子照片 JPG/PNG 文件。</li>
      </ol>
    `
  },
  {
    key: 'family-relationship-proof',
    fileName: '亲属关系证明模板.docx',
    title: '亲属关系证明',
    description: '用于居住在近亲属家中时证明亲属关系。',
    contentHtml: `
      <h1>亲属关系证明</h1>
      <p>兹证明以下人员之间存在亲属关系：</p>
      ${table([
        ['申请人姓名', '张三'],
        ['申请人身份证号', '11010119900307663X'],
        ['亲属姓名', '张某某'],
        ['亲属身份证号', '110101194501011234'],
        ['亲属关系', '父子/母子/夫妻等']
      ])}
      <p class="seal">社区居民委员会（盖章）：____________________</p>
      <p class="seal">日期：______年____月____日</p>
    `
  },
  {
    key: 'community-residence-proof',
    fileName: '社区居住证明模板.docx',
    title: '社区居住证明',
    description: '用于社区开具居民在本辖区居住情况证明。',
    contentHtml: `
      <h1>社区居住证明</h1>
      <p>兹证明 <strong>张三</strong>（身份证号：<strong>11010119900307663X</strong>）现居住于 <strong>XX市XX区XX街道XX小区2号楼202室</strong>。</p>
      <p>该居民自______年____月____日起在本社区连续居住至今。</p>
      <p>特此证明。</p>
      <p class="seal">社区居民委员会（盖章）：____________________</p>
      <p class="seal">日期：______年____月____日</p>
    `
  },
  {
    key: 'no-criminal-record-proof',
    fileName: '无犯罪记录证明模板.docx',
    title: '无犯罪记录证明',
    description: '便民证明类型之一。',
    contentHtml: `
      <h1>无犯罪记录证明</h1>
      <p>兹证明 <strong>张三</strong>，身份证号：<strong>11010119900307663X</strong>，户籍/居住地址为 XX市XX区XX街道XX社区。</p>
      <p>经核查，截至本证明出具之日，未发现其在本辖区有犯罪记录。</p>
      <p>本证明仅用于：____________________。</p>
      <p class="seal">社区居民委员会（盖章）：____________________</p>
      <p class="seal">日期：______年____月____日</p>
    `
  },
  {
    key: 'low-income-hardship-proof',
    fileName: '低收入困难证明模板.docx',
    title: '低收入/困难证明',
    description: '便民证明类型之一。',
    contentHtml: `
      <h1>低收入/困难证明</h1>
      ${table([
        ['申请人', '张三'],
        ['身份证号', '11010119900307663X'],
        ['家庭人口', '3人'],
        ['家庭月收入', '人民币3000元'],
        ['困难原因', '家庭成员患病，医疗支出较高']
      ])}
      <p>经社区核实，该家庭目前生活困难。</p>
      <p class="seal">社区居民委员会（盖章）：____________________</p>
      <p class="seal">日期：______年____月____日</p>
    `
  },
  {
    key: 'same-person-identity-proof',
    fileName: '同一人身份证明模板.docx',
    title: '同一人身份证明',
    description: '用于姓名或证件号码变更前后为同一人的证明。',
    contentHtml: `
      <h1>同一人身份证明</h1>
      ${table([
        ['现姓名', '张三'],
        ['现身份证号', '11010119900307663X'],
        ['曾用名/原证件姓名', '张小三'],
        ['原证件号码', '110101199003071234'],
        ['证明事项', '经核实，上述身份信息对应同一自然人']
      ])}
      <p class="seal">社区居民委员会（盖章）：____________________</p>
      <p class="seal">日期：______年____月____日</p>
    `
  },
  {
    key: 'application-reason-note',
    fileName: '申请事由说明模板.docx',
    title: '申请事由说明',
    description: '便民证明通用材料，用于说明申请证明的用途和原因。',
    contentHtml: `
      <h1>申请事由说明</h1>
      ${table([
        ['申请人', '张三'],
        ['身份证号', '11010119900307663X'],
        ['联系电话', '13800000000'],
        ['申请证明类型', '无犯罪记录证明/低收入困难证明/同一人身份证明'],
        ['申请事由', '请填写具体用途和申请原因']
      ])}
      ${signature()}
    `
  },
  {
    key: 'no-criminal-record-application',
    fileName: '无犯罪记录证明申请表.docx',
    title: '无犯罪记录证明申请表',
    description: '用于无犯罪记录证明申请。',
    contentHtml: `
      <h1>无犯罪记录证明申请表</h1>
      ${table([
        ['申请人', '张三'],
        ['身份证号', '11010119900307663X'],
        ['户籍地址', 'XX市XX区XX街道XX社区'],
        ['现居住地址', 'XX市XX区XX街道XX小区'],
        ['联系电话', '13800000000'],
        ['申请用途', '就业入职/出国签证/志愿者资格审核'],
        ['接收单位', 'XX单位或机构']
      ])}
      <p>本人承诺所填信息真实有效，并同意相关部门依法核验。</p>
      ${signature()}
    `
  },
  {
    key: 'income-statement',
    fileName: '收入说明模板.docx',
    title: '家庭收入情况说明',
    description: '用于低收入/困难证明申请中的收入情况说明。',
    contentHtml: `
      <h1>家庭收入情况说明</h1>
      <p>本人张建国（身份证号：11010119450307663X），家庭人口2人。</p>
      <p>家庭月收入如下：</p>
      <ul>
        <li>本人养老金：3,200元/月</li>
        <li>配偶：无收入</li>
        <li>子女赡养费：200元/月</li>
      </ul>
      <p>合计：3,400元/月。</p>
      <p>本人承诺以上信息真实有效。</p>
      ${signature()}
    `
  },
  {
    key: 'family-situation-statement',
    fileName: '家庭情况说明模板.docx',
    title: '家庭情况说明',
    description: '用于说明因病、因残、因灾、失业等致困原因。',
    contentHtml: `
      <h1>家庭情况说明</h1>
      ${table([
        ['申请人', '张建国'],
        ['身份证号', '11010119450307663X'],
        ['家庭人口', '2人'],
        ['主要困难原因', '因病/因残/因灾/失业/教育支出较高'],
        ['目前生活状况', '请描述家庭收入、支出和实际困难情况'],
        ['申请事项', '低收入/困难证明']
      ])}
      ${signature()}
    `
  },
  {
    key: 'low-income-family-application',
    fileName: '低收入家庭申请表.docx',
    title: '低收入家庭申请表',
    description: '社区/街道统一制式低收入或困难证明申请表。',
    contentHtml: `
      <h1>低收入家庭申请表</h1>
      ${table([
        ['申请人', '张建国'],
        ['身份证号', '11010119450307663X'],
        ['联系电话', '13900000000'],
        ['家庭住址', 'XX市XX区XX街道XX小区'],
        ['家庭成员情况', '请填写姓名、关系、年龄、就业及收入情况'],
        ['申请原因', '请填写申请低收入/困难证明的具体原因']
      ])}
      <p>本人承诺所填家庭人口、收入和资产情况真实有效。</p>
      ${signature()}
    `
  },
  {
    key: 'marital-status-commitment',
    fileName: '婚姻状况说明承诺书.docx',
    title: '婚姻状况说明承诺书',
    description: '适用于无法提供完整婚姻登记档案的特殊情形。',
    contentHtml: `
      <h1>婚姻状况说明承诺书</h1>
      ${table([
        ['申请人', '张三'],
        ['身份证号', '11010119900307663X'],
        ['当前婚姻状况', '未婚/已婚/离异/丧偶'],
        ['证明用途', '补办证件/户口迁移/财产继承/再婚登记/贷款审批'],
        ['情况说明', '请说明无法获取档案或需补充说明的原因']
      ])}
      <p>本人承诺以上说明真实有效，并愿意承担因虚假声明产生的一切法律责任。</p>
      ${signature('承诺人签字')}
    `
  },
  {
    key: 'same-person-identity-statement',
    fileName: '同一人身份声明书.docx',
    title: '同一人身份声明书',
    description: '用于姓名或身份证号变更前后为同一人的本人声明。',
    contentHtml: `
      <h1>同一人身份声明书</h1>
      <p>本人张三（公民身份号码：11010119900307663X），郑重声明：</p>
      ${table([
        ['曾使用姓名', '张某某'],
        ['曾使用身份证号', '110101900307663（15位）'],
        ['变更原因', '身份证号升级（15位转18位）/姓名变更/档案记录错误/户籍迁移'],
        ['声明事项', '上述两个身份信息均为本人']
      ])}
      <p>本人承诺以上声明真实有效，并愿意承担因虚假声明产生的一切法律责任。</p>
      ${signature('声明人签字')}
    `
  },
  {
    key: 'proxy-authorization',
    fileName: '委托书模板.docx',
    title: '委托书',
    description: '用于委托他人代办证明事项。',
    contentHtml: `
      <h1>委托书</h1>
      ${table([
        ['委托人', '张三'],
        ['委托人身份证号', '11010119900307663X'],
        ['受托人', '李四'],
        ['受托人身份证号', '110101198801011234'],
        ['委托事项', '代办便民证明相关申请'],
        ['委托期限', '自______年____月____日至______年____月____日']
      ])}
      ${signature('委托人签字')}
      ${signature('受托人签字')}
    `
  },
  {
    key: 'meal-service-application',
    fileName: '助餐服务申请表.docx',
    title: '助餐服务申请表',
    description: '用于助餐服务预约，填写用餐需求和紧急联系人。',
    contentHtml: `
      <h1>助餐服务申请表</h1>
      ${table([
        ['申请人姓名', '张某某'],
        ['身份证号', '110101194501011234'],
        ['年龄', '80周岁'],
        ['联系电话', '13900000000'],
        ['居住地址', 'XX市XX区XX街道XX小区'],
        ['助餐类型', '午餐/晚餐'],
        ['就餐方式', '到店用餐/送餐到家'],
        ['紧急联系人', '张三/子女/13800000000']
      ])}
      <p>本人承诺以上信息真实有效，并同意遵守社区助餐服务相关规定。</p>
      ${signature()}
    `
  }
]

export function resolveBuiltInMaterialTemplate(materialName: string, materialType?: string, description?: string) {
  const source = `${materialName} ${materialType || ''} ${description || ''}`
  const rules: Array<[RegExp, string]> = [
    [/房屋租赁合同|rental_contract/i, 'house-rental-contract'],
    [/不动产权证书|房屋产权证明文件|房产证|real_estate_certificate/i, 'real-estate-certificate'],
    [/购房合同|purchase_contract/i, 'purchase-contract'],
    [/住宿证明|单位住宿证明|用人单位\/就读学校出具的住宿证明|unit_accommodation_proof/i, 'unit-accommodation-proof'],
    [/工商营业执照|营业执照|business_license/i, 'business-license'],
    [/劳动合同|labor_contract/i, 'labor-contract'],
    [/劳动关系证明|labor_relation_proof/i, 'labor-relation-proof'],
    [/学生证|student_card/i, 'student-card'],
    [/连续就读证明|continuous_study_proof/i, 'continuous-study-proof'],
    [/高龄津贴申请表|老年补贴申请表|senior_allowance_application/i, 'senior-allowance-application'],
    [/近期免冠|两寸照片|本人相片|证件照|personal_photo|recent_two_inch_photo|photo/i, 'photo-requirement'],
    [/亲属关系证明|family_relationship_proof/i, 'family-relationship-proof'],
    [/社区居住证明|community_residence_proof/i, 'community-residence-proof'],
    [/无犯罪记录证明申请表|no_criminal_record_application/i, 'no-criminal-record-application'],
    [/收入说明|income_statement/i, 'income-statement'],
    [/家庭情况说明|family_situation_statement/i, 'family-situation-statement'],
    [/低收入家庭申请表|low_income_family_application/i, 'low-income-family-application'],
    [/婚姻状况说明承诺书|marital_status_commitment/i, 'marital-status-commitment'],
    [/同一人身份声明书|same_person_identity_statement/i, 'same-person-identity-statement'],
    [/委托书|proxy_authorization/i, 'proxy-authorization'],
    [/无犯罪记录证明|no_criminal_record_proof/i, 'no-criminal-record-proof'],
    [/低收入|困难证明|low_income_hardship_proof/i, 'low-income-hardship-proof'],
    [/同一人身份证明|same_person_identity_proof/i, 'same-person-identity-proof'],
    [/申请事由说明|application_reason_note/i, 'application-reason-note'],
    [/助餐服务申请表|meal_service_application/i, 'meal-service-application']
  ]
  const found = rules.find(([pattern]) => pattern.test(source))
  return found ? builtInMaterialTemplates.find(template => template.key === found[1]) : undefined
}

export function isIdentityDocumentMaterial(materialName: string, materialType?: string) {
  return /身份证|居民身份证|户口簿|户口本|社保卡|银行卡|居住证|id_card|household_register|social_security_card|bank_card|residence_permit_card/i
    .test(`${materialName} ${materialType || ''}`)
}

export function wrapMaterialTemplateHtml(template: BuiltInMaterialTemplate) {
  return `
<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <title>${template.title}</title>
  <style>
    body {
      max-width: 860px;
      margin: 0 auto;
      padding: 40px 48px;
      font-family: "Microsoft YaHei", Arial, sans-serif;
      color: #111827;
      line-height: 1.8;
      background: #fff;
    }
    h1 { text-align: center; font-size: 26px; margin-bottom: 24px; }
    table { width: 100%; border-collapse: collapse; margin: 14px 0 22px; }
    th, td { border: 1px solid #9ca3af; padding: 10px 12px; vertical-align: top; }
    th { width: 28%; background: #f3f4f6; text-align: left; }
    .seal { margin-top: 16px; text-align: right; }
    .signature { margin-top: 24px; }
    .note { color: #4b5563; background: #f9fafb; padding: 10px 12px; border-left: 4px solid #d4a843; }
    ol { padding-left: 24px; }
  </style>
</head>
<body>
  ${template.contentHtml}
</body>
</html>`
}
